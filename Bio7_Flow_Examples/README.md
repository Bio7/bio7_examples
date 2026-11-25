# Flow examples

| Folder | Contents |
|---|---|
| [Models](Models) | Five flows which simulate and analyse an ecological model with Java, R and Python |
| [Examples](Examples) | Flows with BeanShell scripts and an ImageJ macro: decisions, pauses, a loop over images |
| [Components](Components) | Reusable components: dialogs, decisions, pauses |

Flows have the extension `.flow` (flows of older versions, `.ecoflow`, are opened and run, too).
A flow stores the workspace paths of its scripts (e.g. `/Bio7_Flow_Examples/Models/1_Harvest/...`):
keep the project names.

## The model flows

Each flow runs a model, its analysis and its plots, one language after the other. The steps share
their data in memory: Java sends data frames to R (`RUtil.dataFrameToR`), Python reads R objects as
pandas DataFrames (`bio7.r("name")`), Java reads Python data (`PythonUtil.fromPythonDoubles`). R and
Python start when a step needs them. The Java models are in Bio7_Java_Examples (package
`flow.models`): a Java step of a flow is compiled in its Java project.

| Flow | Model | Steps |
|---|---|---|
| [1_Harvest](Models/1_Harvest) | A fish stock with logistic growth, noise and a constant harvest rate | Java: 30 runs for 25 rates; R: the rate of the highest catch, the collapse risk (glm); Python: catch and risk |
| [2_Enrichment](Models/2_Enrichment) | The paradox of enrichment: the predator-prey model of Rosenzweig and MacArthur | Java: Runge-Kutta for carrying capacities K; R: stable or cycling, the Hopf bifurcation, the period; Python: bifurcation diagram, phase plane |
| [3_Metapopulation](Models/3_Metapopulation) | The incidence function model (Hanski 1994) in a fragmented landscape | Python: 60 patches; Java: colonisation and extinction; R: incidence ~ area + connectivity (glm); Python: map, occupancy |
| [4_Sea_Turtle](Models/4_Sea_Turtle) | A stage matrix of the loggerhead sea turtle (after Crouse et al. 1987) | R: lambda, sensitivities, elasticities; Java: stochastic projections of three management scenarios; Python: elasticities, quasi-extinction |
| [5_Wildlife_Disease](Models/5_Wildlife_Disease) | A stochastic SIR model (Gillespie) of a disease in a wildlife population | Java: 100 epidemics per vaccination coverage; R: R0, the critical coverage 1 - 1/R0; Python: epidemic curves, herd immunity |

Requires: numpy, pandas and matplotlib in Python; R without extra packages.

**Run:** open a flow and start it (Run in the flow menu). The executed step is highlighted (Debug
Mode, on by default). Cancel (Ctrl+Alt+Shift+X) also interrupts a running R or Python script or Java
model.
