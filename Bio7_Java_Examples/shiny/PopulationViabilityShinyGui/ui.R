# The user interface of the Shiny app, created by Bio7 (WindowBuilder Shiny)
# from the GUI class PopulationViabilityShinyGui.java (/Bio7_Java_Examples/src/shinygui).
# Change the GUI with the WindowBuilder and create the app again: this file is
# written again (the code of server.R stays).
library(shiny)

fluidPage(
  titlePanel("Population Viability"),
  fluidRow(
    column(3,
      tags$label("Birth rate (1/100)")
    ),
    column(3,
      sliderInput("scaleBirth", NULL, min = 5, max = 100, value = 40)
    ),
    column(3,
      tags$label("Death rate (1/100)")
    ),
    column(3,
      sliderInput("scaleDeath", NULL, min = 5, max = 100, value = 30)
    )
  ),
  fluidRow(
    column(3,
      tags$label("Catastrophes (1/100 per year)")
    ),
    column(3,
      sliderInput("scaleCatastrophe", NULL, min = 0, max = 20, value = 2)
    ),
    column(3,
      tags$label("Carrying capacity")
    ),
    column(3,
      numericInput("spinnerK", NULL, value = 100, min = 10, max = 5000)
    )
  ),
  fluidRow(
    column(3,
      tags$label("Population at the start")
    ),
    column(3,
      numericInput("spinnerN0", NULL, value = 20, min = 1, max = 5000)
    ),
    column(3,
      tags$label("Years")
    ),
    column(3,
      numericInput("spinnerYears", NULL, value = 100, min = 10, max = 1000)
    )
  ),
  fluidRow(
    column(3,
      tags$label("Runs")
    ),
    column(3,
      numericInput("spinnerRuns", NULL, value = 200, min = 10, max = 5000)
    ),
    column(6,
      wellPanel(
        h4("Axis of the population"),
        flowLayout(
          radioButtons("grpScale", NULL, choices = c("Linear", "Logarithmic"), selected = "Linear")
        )
      )
    )
  ),
  fluidRow(
    column(12,
      div(
        flowLayout(
          actionButton("btnRun", "New runs")
        )
      )
    )
  ),
  fluidRow(
    column(6,
      plotOutput("canvasRuns")
    ),
    column(6,
      plotOutput("canvasRisk")
    )
  ),
  fluidRow(
    column(12,
      textOutput("outSummary")
    )
  ),
  fluidRow(
    column(12,
      tableOutput("tableQuantiles")
    )
  )
)
