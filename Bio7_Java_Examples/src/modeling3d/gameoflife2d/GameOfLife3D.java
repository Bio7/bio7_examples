package modeling3d.gameoflife2d;

import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.Overlay;
import com.jogamp.opengl.util.gl2.GLUT;
import com.eco.bio7.spatial.SpatialUtil;
import com.eco.bio7.spatial.SpatialEvents;
import com.eco.bio7.collection.CustomView;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import java.awt.*;
import java.util.concurrent.*;

/**
 * CONWAY'S GAME OF LIFE - 2D CANVAS (CENTERED & ZOOMABLE)
 * 
 * --- FEATURES --- - Simulation panel centered in viewport - Zoom in/out (cells
 * extend outside viewport) - No black areas - only simulation content -
 * Multi-threaded calculation - Fast rendering - Supports grids up to 5000x5000
 * - Mouse drag to pan, wheel to zoom
 */

public class GameOfLife3D extends com.eco.bio7.compile.Model {

	// --- GRID PARAMETERS ---
	public int GRID_WIDTH = 256;
	public int GRID_HEIGHT = 256;

	// --- SIMULATION PARAMETERS ---
	public int SPEED = 1;
	public double INITIAL_POPULATION = 0.3;

	// --- ZOOM & PAN CONTROLS ---
	public double ZOOM = 1.0; // Cell size in pixels
	public int OFFSET_X = 0; // Pan offset in pixels
	public int OFFSET_Y = 0;

	// --- GRID STATE ---
	private volatile boolean[][] grid;
	private volatile boolean[][] nextGrid;
	private volatile boolean gridNeedsReset = false;
	private int generation = 0;

	// --- DISPLAY BUFFER ---
	private int[] pixelBuffer;
	private volatile boolean pixelBufferNeedsUpdate = true;
	private int displayWidth = 512;
	private int displayHeight = 512;
	private int panelWidth = 0; // Actual panel size
	private int panelHeight = 0;
	private int panelOffsetX = 0; // Centered position
	private int panelOffsetY = 0;

	// --- STATISTICS ---
	private volatile int livingCells = 0;
	private int[] populationHistory = new int[200];
	private int historyIndex = 0;

	// --- THREADING ---
	private ExecutorService executorService;
	private int numThreads;

	// --- OVERLAY ---
	public Overlay statsOverlay;
	private Font titleFont = new Font("SansSerif", Font.BOLD, 24);

	// --- PAN & MOUSE CONTROL ---
	private int baseOffsetX = 0; // Store base position
	private int baseOffsetY = 0; // Store base position
	private int lastMouseX = 0;
	private int lastMouseY = 0;
	private boolean isDragging = false;

	/* Start after compilation if run action is active! */
	public GameOfLife3D() {
		setup();
	}

	public void setup() {
		setupModel();
	}

	private void setupModel() {
		if (!SpatialUtil.isStarted())
			SpatialUtil.startStop();

		numThreads = Runtime.getRuntime().availableProcessors();
		executorService = Executors.newFixedThreadPool(numThreads);
		statsOverlay = SpatialUtil.createOverlay();

		initializeGrid();
		createGui();
	}

	private void createGui() {
		CustomView view = new CustomView();
		Display d = Display.getDefault();
		d.syncExec(() -> {
			Composite parent = view.getComposite("Game of Life Control Panel", null);
			parent.setLayout(new FillLayout());
			new GameOfLifeSettings(parent, GameOfLife3D.this);
			parent.layout(true);
		});
	}

	/**
	 * Initialize or reinitialize the grid with current dimensions
	 */
	public synchronized void initializeGrid() {
		grid = new boolean[GRID_HEIGHT][GRID_WIDTH];
		nextGrid = new boolean[GRID_HEIGHT][GRID_WIDTH];

		for (int y = 0; y < GRID_HEIGHT; y++) {
			for (int x = 0; x < GRID_WIDTH; x++) {
				grid[y][x] = Math.random() < INITIAL_POPULATION;
			}
		}

		generation = 0;
		countLivingCells();
		gridNeedsReset = false;
		pixelBufferNeedsUpdate = true;

		for (int i = 0; i < populationHistory.length; i++) {
			populationHistory[i] = livingCells;
		}
	}

	public void requestGridReset() {
		gridNeedsReset = true;
	}

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		setupModel();
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {
		// Handle Bio7 mouse events for panning and zooming
		handleMouseEvents();

		if (gridNeedsReset) {
			initializeGrid();
		}

		if (SpatialUtil.canStep()) {
			try {
				for (int i = 0; i < SPEED; i++) {
					updateGenerationMultiThreaded();
				}
				countLivingCells();
				updateStatistics();

			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("Bounds error: " + e.getMessage());
				initializeGrid();
			}
		}
		
		pixelBufferNeedsUpdate = true;
		render(gl, glut);
		drawStatsOverlay(gl, glu, glut);
	}

	/**
	 * Handle Bio7 mouse events for panning and zooming
	 */
	private void handleMouseEvents() {
		// Mouse drag for panning
		if (SpatialEvents.isMouseDragged()) {
			com.jogamp.newt.event.MouseEvent e = SpatialEvents.getDragEvent();
			int x = e.getX();
			int y = e.getY();

			if (!isDragging) {
				// First drag - store the current offsets as base
				isDragging = true;
				baseOffsetX = OFFSET_X;
				baseOffsetY = OFFSET_Y;
				lastMouseX = x;
				lastMouseY = y;
			} else {
				// Continue dragging - calculate delta from drag start
				int deltaX = x - lastMouseX;
				int deltaY = -(y - lastMouseY); // Invert Y

				OFFSET_X = baseOffsetX + deltaX;
				OFFSET_Y = baseOffsetY + deltaY;
			}
		}

		// Mouse released - stop dragging and update base position
		if (SpatialEvents.isMousePressed()) {
			if (isDragging) {
				baseOffsetX = OFFSET_X;
				baseOffsetY = OFFSET_Y;
				isDragging = false;
			}
		}

		// Double-click to reset offset to center
		if (SpatialEvents.isMouseDoubleClicked()) {
			OFFSET_X = 0;
			OFFSET_Y = 0;
			baseOffsetX = 0;
			baseOffsetY = 0;
			isDragging = false;
		}

		if (SpatialEvents.isMouseWheelMoved()) {
			com.jogamp.newt.event.MouseEvent e = SpatialEvents.getMouseWheelEvent();
			float[] rotation = e.getRotation();
			float verticalRotation = rotation[1]; // Typically vertical
			if (verticalRotation > 0.0f) {
				ZOOM *= 1.1; // Zoom in
			} else if (verticalRotation < 0.0f) {
				ZOOM /= 1.1; // Zoom out
			}

			// Clamp zoom between 0.5x and 100x
			ZOOM = Math.max(0.5, Math.min(100.0, ZOOM));
		}

		// Right-click to clear grid
		if (SpatialEvents.isRightMouseClicked()) {
			clearAllCells();
			isDragging = false;
		}

		// Triple-click to reset everything
		if (SpatialEvents.isMouseTripleClicked()) {
			OFFSET_X = 0;
			OFFSET_Y = 0;
			baseOffsetX = 0;
			baseOffsetY = 0;
			ZOOM = 1.0;
			isDragging = false;
		}
	}

	private void updateGenerationMultiThreaded() {
		synchronized (grid) {
			for (int y = 0; y < GRID_HEIGHT; y++) {
				for (int x = 0; x < GRID_WIDTH; x++) {
					nextGrid[y][x] = grid[y][x];
				}
			}
		}

		int chunkHeight = Math.max(1, GRID_HEIGHT / numThreads);
		java.util.List<Future<?>> futures = new java.util.ArrayList<>();

		for (int t = 0; t < numThreads; t++) {
			final int threadId = t;
			futures.add(executorService.submit(() -> {
				int startY = threadId * chunkHeight;
				int endY = (threadId == numThreads - 1) ? GRID_HEIGHT : (threadId + 1) * chunkHeight;

				for (int y = startY; y < endY; y++) {
					if (y >= GRID_HEIGHT)
						break;
					for (int x = 0; x < GRID_WIDTH; x++) {
						if (x >= GRID_WIDTH)
							continue;

						int neighbors = countNeighbors(x, y);
						boolean alive = grid[y][x];

						if (alive && neighbors < 2) {
							nextGrid[y][x] = false;
						} else if (alive && neighbors > 3) {
							nextGrid[y][x] = false;
						} else if (!alive && neighbors == 3) {
							nextGrid[y][x] = true;
						}
					}
				}
			}));
		}

		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			System.err.println("Multi-threading error: " + e.getMessage());
		}

		synchronized (grid) {
			boolean[][] temp = grid;
			grid = nextGrid;
			nextGrid = temp;
		}

		generation++;
	}

	private int countNeighbors(int x, int y) {
		int count = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dy == 0)
					continue;
				int ny = (y + dy + GRID_HEIGHT) % GRID_HEIGHT;
				int nx = (x + dx + GRID_WIDTH) % GRID_WIDTH;
				if (ny >= 0 && ny < GRID_HEIGHT && nx >= 0 && nx < GRID_WIDTH) {
					if (grid[ny][nx])
						count++;
				}
			}
		}
		return count;
	}

	private void countLivingCells() {
		livingCells = 0;
		synchronized (grid) {
			for (int y = 0; y < GRID_HEIGHT; y++) {
				for (int x = 0; x < GRID_WIDTH; x++) {
					if (grid[y][x])
						livingCells++;
				}
			}
		}
	}

	private void updateStatistics() {
		populationHistory[historyIndex] = livingCells;
		historyIndex = (historyIndex + 1) % populationHistory.length;
	}

	/**
	 * Build pixel buffer with zoom and pan Only renders visible portion of grid
	 */
	private void buildPixelBuffer() {
		// Clear background
		java.util.Arrays.fill(pixelBuffer, 0xFF000000);

		// Calculate cell size in pixels
		int cellSizePixels = Math.max(1, (int) ZOOM);

		// Calculate grid position relative to panel center
		int gridStartX = panelOffsetX + OFFSET_X;
		int gridStartY = panelOffsetY + OFFSET_Y;

		synchronized (grid) {
			for (int y = 0; y < GRID_HEIGHT; y++) {
				for (int x = 0; x < GRID_WIDTH; x++) {
					if (!grid[y][x])
						continue;

					// Calculate pixel position
					int pixelX = gridStartX + x * cellSizePixels;
					int pixelY = gridStartY + y * cellSizePixels;

					// Draw cell (clipped to display)
					int startPixelX = Math.max(0, pixelX);
					int startPixelY = Math.max(0, pixelY);
					int endPixelX = Math.min(displayWidth, pixelX + cellSizePixels);
					int endPixelY = Math.min(displayHeight, pixelY + cellSizePixels);

					for (int py = startPixelY; py < endPixelY; py++) {
						for (int px = startPixelX; px < endPixelX; px++) {
							int bufferIndex = py * displayWidth + px;
							if (bufferIndex >= 0 && bufferIndex < pixelBuffer.length) {
								pixelBuffer[bufferIndex] = 0xFF00FF00; // Green
							}
						}
					}
				}
			}
		}

		pixelBufferNeedsUpdate = false;
	}

	private void render(GL2 gl, GLUT glut) {
		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_DEPTH_TEST);

		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();

		// Get current viewport size
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL_VIEWPORT, viewport, 0);
		displayWidth = Math.max(1, viewport[2]);
		displayHeight = Math.max(1, viewport[3]);

		// Calculate panel size based on zoom
		int cellSizePixels = Math.max(1, (int) ZOOM);
		panelWidth = GRID_WIDTH * cellSizePixels;
		panelHeight = GRID_HEIGHT * cellSizePixels;

		// Calculate centered position
		panelOffsetX = (displayWidth - panelWidth) / 2;
		panelOffsetY = (displayHeight - panelHeight) / 2;

		// Reallocate pixel buffer if viewport changed
		if (pixelBuffer == null || pixelBuffer.length != displayWidth * displayHeight) {
			pixelBuffer = new int[displayWidth * displayHeight];
			pixelBufferNeedsUpdate = true;
		}

		// Rebuild pixel buffer if needed
		if (pixelBufferNeedsUpdate) {
			buildPixelBuffer();
		}

		// Setup orthographic projection
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, displayWidth, displayHeight, 0, -1, 1);

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();

		// Render pixel buffer directly
		gl.glWindowPos2i(0, 0);
		gl.glPixelZoom(1.0f, 1.0f);

		gl.glDrawPixels(displayWidth, displayHeight, GL_RGBA, GL_UNSIGNED_BYTE,
				java.nio.ByteBuffer.wrap(toByteArray(pixelBuffer)));

		// Restore matrices
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPopMatrix();

		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_LIGHTING);
	}

	/**
	 * Convert int[] ARGB to byte[] RGBA
	 */
	private byte[] toByteArray(int[] pixels) {
		byte[] bytes = new byte[pixels.length * 4];
		for (int i = 0; i < pixels.length; i++) {
			int pixel = pixels[i];
			bytes[i * 4] = (byte) ((pixel >> 16) & 0xFF); // R
			bytes[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF); // G
			bytes[i * 4 + 2] = (byte) (pixel & 0xFF); // B
			bytes[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // A
		}
		return bytes;
	}

	private void drawStatsOverlay(GL2 gl, GLU glu, GLUT glut) {
		if (statsOverlay == null)
			return;

		if (SpatialUtil.isSplitView()) {
			if (SpatialUtil.isSplitPanelDrawing())
				return;
		}

		Graphics2D g2d = statsOverlay.createGraphics();

		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f);
		g2d.setComposite(ac);
		g2d.setColor(new Color(15, 15, 25));
		g2d.fillRect(10, 10, 950, 220);

		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.drawRect(10, 10, 950, 220);

		g2d.setFont(titleFont);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Conway's Game of Life (" + numThreads + " cores)", 25, 40);

		// Larger info font
		Font largeInfoFont = new Font("SansSerif", Font.BOLD, 16);
		g2d.setFont(largeInfoFont);

		g2d.setColor(new Color(0, 255, 0));
		g2d.drawString("●", 25, 75);
		g2d.setColor(Color.WHITE);
		g2d.drawString(" Living: " + livingCells, 40, 75);

		g2d.setColor(new Color(255, 255, 0));
		g2d.drawString("■", 25, 105);
		g2d.setColor(Color.WHITE);
		g2d.drawString(" Gen: " + generation, 40, 105);

		g2d.setColor(new Color(50, 200, 255));
		g2d.drawString("◆", 25, 135);
		g2d.setColor(Color.WHITE);
		g2d.drawString(" Grid: " + GRID_WIDTH + "×" + GRID_HEIGHT, 40, 135);

		g2d.setColor(new Color(200, 75, 255));
		g2d.drawString("◇", 25, 165);
		g2d.setColor(Color.WHITE);

		g2d.drawString(" Speed: " + SPEED + "x | Zoom: " + String.format("%.2f", ZOOM) + "x", 40, 165);

		// Draw pie chart beside statistics
		drawPieChart(g2d, livingCells, GRID_WIDTH * GRID_HEIGHT - livingCells);

		statsOverlay.markDirty(10, 10, 950, 220);
		statsOverlay.drawAll();
		g2d.dispose();
	}

	/**
	 * Draw compact pie chart showing alive vs dead cells
	 */
	private void drawPieChart(Graphics2D g2d, int aliveCells, int deadCells) {
		int totalCells = aliveCells + deadCells;
		if (totalCells == 0)
			return;

		// Pie chart position and size (compact)
		int pieX = 520;
		int pieY = 35;
		int pieSize = 130;

		// Draw background circle
		g2d.setColor(new Color(40, 40, 50));
		g2d.fillOval(pieX, pieY, pieSize, pieSize);

		// Draw alive cells (green)
		g2d.setColor(new Color(0, 255, 0));
		double alivePercentage = (aliveCells * 100.0) / totalCells;
		double aliveAngle = (alivePercentage / 100.0) * 360.0;
		g2d.fillArc(pieX, pieY, pieSize, pieSize, 0, (int) aliveAngle);

		// Draw dead cells (red)
		g2d.setColor(new Color(255, 50, 50));
		g2d.fillArc(pieX, pieY, pieSize, pieSize, (int) aliveAngle, 360 - (int) aliveAngle);

		// Draw border
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.drawOval(pieX, pieY, pieSize, pieSize);

		// Draw legend below pie chart - properly positioned inside box
		int legendX = pieX - 15;
		int legendY = pieY + pieSize + 8;

		g2d.setFont(new Font("SansSerif", Font.BOLD, 12));

		// Alive legend
		g2d.setColor(new Color(0, 255, 0));
		g2d.fillRect(legendX, legendY, 11, 11);
		g2d.setColor(Color.WHITE);
		g2d.drawString("Alive " + String.format("%.1f%%", alivePercentage), legendX + 16, legendY + 9);

		// Dead legend - positioned with proper vertical spacing
		g2d.setColor(new Color(255, 50, 50));
		g2d.fillRect(legendX, legendY + 16, 11, 11);
		g2d.setColor(Color.WHITE);
		double deadPercentage = 100.0 - alivePercentage;
		g2d.drawString("Dead " + String.format("%.1f%%", deadPercentage), legendX + 16, legendY + 24);
	}

	/**
	 * Load a pattern into the grid with proper spacing
	 */
	public synchronized void loadPattern(int[][] pattern) {
		clearAllCells();

		if (pattern == null || pattern.length == 0) {
			return;
		}

		// Find bounds of pattern
		int minRow = Integer.MAX_VALUE;
		int maxRow = Integer.MIN_VALUE;
		int minCol = Integer.MAX_VALUE;
		int maxCol = Integer.MIN_VALUE;

		for (int[] cell : pattern) {
			minRow = Math.min(minRow, cell[0]);
			maxRow = Math.max(maxRow, cell[0]);
			minCol = Math.min(minCol, cell[1]);
			maxCol = Math.max(maxCol, cell[1]);
		}

		// Add EXTRA padding for spaceships to move
		int paddingLeft = 50;
		int paddingTop = 50;

		int startRow = paddingTop;
		int startCol = paddingLeft;

		// Place cells
		for (int[] cell : pattern) {
			int row = startRow + (cell[0] - minRow);
			int col = startCol + (cell[1] - minCol);

			if (row >= 0 && row < GRID_HEIGHT && col >= 0 && col < GRID_WIDTH) {
				grid[row][col] = true;
			}
		}

		generation = 0;
		countLivingCells();
		pixelBufferNeedsUpdate = true;
	}

	/**
	 * Clear all cells (empty grid)
	 */
	public synchronized void clearAllCells() {
		for (int y = 0; y < GRID_HEIGHT; y++) {
			for (int x = 0; x < GRID_WIDTH; x++) {
				grid[y][x] = false;
			}
		}
		generation = 0;
		countLivingCells();
		pixelBufferNeedsUpdate = true;
	}
}