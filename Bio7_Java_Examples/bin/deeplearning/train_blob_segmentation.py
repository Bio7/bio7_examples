"""
Trains a small U-Net in PyTorch which segments bright blobs (cells, nuclei,
particles) on a dark, noisy background and exports it to ONNX for the Bio7
example DeepLearningSegmentation.java (Deep Java Library, ONNX Runtime engine).

The model:
  input  "image":  float32 [N, 1, H, W], each image scaled to 0 - 1 (min - max)
  output "logits": float32 [N, 2, H, W], class 0 = background, class 1 = blob
  H and W: any size that is a multiple of 4 (the example pads the image)

Usage: python train_blob_segmentation.py [output.onnx]
Requires: torch (the ONNX export of torch, no other package)
"""
import math
import sys

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

SIZE = 64
torch.manual_seed(1)
torch.set_num_threads(max(1, torch.get_num_threads()))
rng = np.random.default_rng(1)
BLUR = torch.tensor([[1, 2, 1], [2, 4, 2], [1, 2, 1]], dtype=torch.float32).view(1, 1, 3, 3) / 16


def batch(n, size=SIZE):
    """Synthetic images with blobs of different size and brightness (vectorized) and their masks."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    images = np.zeros((n, size, size), np.float32)
    masks = np.zeros((n, size, size), np.float32)
    for k in range(n):
        for _ in range(rng.integers(2, 8)):
            cx, cy = rng.uniform(0, size, 2)
            rx, ry = rng.uniform(3, 10, 2)
            angle = rng.uniform(0, math.pi)
            c, s = math.cos(angle), math.sin(angle)
            u = ((xx - cx) * c + (yy - cy) * s) / rx
            v = (-(xx - cx) * s + (yy - cy) * c) / ry
            r2 = u * u + v * v
            inside = r2 <= 1.0
            brightness = rng.uniform(0.35, 1.0)
            images[k] = np.where(inside, np.maximum(images[k], brightness * (1.0 - 0.3 * r2)), images[k])
            masks[k][inside] = 1.0
        # an uneven background and noise
        images[k] += rng.uniform(0, 0.25) * (xx / size) + rng.uniform(0, 0.15)
        images[k] += rng.normal(0, rng.uniform(0.02, 0.12), (size, size)).astype(np.float32)
    x = F.conv2d(torch.from_numpy(images)[:, None], BLUR, padding=1)
    # each image scaled to 0 - 1 (min - max, like the example in Bio7)
    lo = x.amin(dim=(2, 3), keepdim=True)
    hi = x.amax(dim=(2, 3), keepdim=True)
    x = (x - lo) / (hi - lo + 1e-6)
    return x, torch.from_numpy(masks).long()


def block(cin, cout):
    return nn.Sequential(nn.Conv2d(cin, cout, 3, padding=1), nn.BatchNorm2d(cout), nn.ReLU(inplace=True),
                         nn.Conv2d(cout, cout, 3, padding=1), nn.BatchNorm2d(cout), nn.ReLU(inplace=True))


class UNet(nn.Module):
    def __init__(self, c=8):
        super().__init__()
        self.e1, self.e2, self.e3 = block(1, c), block(c, 2 * c), block(2 * c, 4 * c)
        self.d2, self.d1 = block(6 * c, 2 * c), block(3 * c, c)
        self.out = nn.Conv2d(c, 2, 1)

    def forward(self, x):
        e1 = self.e1(x)
        e2 = self.e2(F.max_pool2d(e1, 2))
        e3 = self.e3(F.max_pool2d(e2, 2))
        d2 = self.d2(torch.cat([F.interpolate(e3, scale_factor=2.0, mode="nearest"), e2], 1))
        d1 = self.d1(torch.cat([F.interpolate(d2, scale_factor=2.0, mode="nearest"), e1], 1))
        return self.out(d1)


def main():
    output = sys.argv[1] if len(sys.argv) > 1 else "blob_segmentation.onnx"
    model = UNet()
    optimizer = torch.optim.Adam(model.parameters(), lr=3e-3)
    model.train()
    for step in range(300):
        x, y = batch(16)
        loss = F.cross_entropy(model(x), y)
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
        if step % 50 == 0:
            print(f"step {step}: loss {loss.item():.4f}", flush=True)
    model.eval()
    # the accuracy on new images (intersection over union of the blobs)
    with torch.no_grad():
        x, y = batch(32, 128)
        p = model(x).argmax(1)
        iou = ((p == 1) & (y == 1)).sum().item() / max(1, ((p == 1) | (y == 1)).sum().item())
    print(f"test IoU of the blobs: {iou:.3f}")
    dummy = torch.zeros(1, 1, 64, 64)
    # opset 18: the minimum of the exporter of torch >= 2.9 (a lower one fails to convert);
    # ONNX Runtime (DJL) runs it
    torch.onnx.export(model, dummy, output, input_names=["image"], output_names=["logits"], opset_version=18,
                      dynamic_axes={"image": {0: "batch", 2: "height", 3: "width"},
                                    "logits": {0: "batch", 2: "height", 3: "width"}})
    print("exported", output)


if __name__ == "__main__":
    main()
