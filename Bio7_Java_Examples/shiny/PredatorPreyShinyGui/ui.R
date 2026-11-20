# The user interface of the Shiny app, created by Bio7 (WindowBuilder Shiny)
# from the GUI class PredatorPreyShinyGui.java.
# Change the GUI with the WindowBuilder and create the app again: this file is
# written again (the code of server.R stays).
library(shiny)

fluidPage(
  titlePanel("Predator and Prey"),
  fluidRow(
    column(6,
      wellPanel(
        h4("Prey"),
        fluidRow(
          column(6,
            tags$label("Growth rate r")
          ),
          column(6,
            sliderInput("scaleR", NULL, min = 10, max = 200, value = 100)
          )
        ),
        fluidRow(
          column(6,
            tags$label("Carrying capacity K")
          ),
          column(6,
            sliderInput("scaleK", NULL, min = 50, max = 1000, value = 500, step = 10)
          )
        ),
        fluidRow(
          column(6,
            tags$label("Prey at the start")
          ),
          column(6,
            numericInput("spinnerN0", NULL, value = 100, min = 1, max = 1000)
          )
        )
      )
    ),
    column(6,
      wellPanel(
        h4("Predator"),
        fluidRow(
          column(6,
            tags$label("Attack rate a (1/1000)")
          ),
          column(6,
            sliderInput("scaleA", NULL, min = 1, max = 50, value = 10)
          )
        ),
        fluidRow(
          column(6,
            tags$label("Conversion b")
          ),
          column(6,
            sliderInput("scaleB", NULL, min = 5, max = 100, value = 50)
          )
        ),
        fluidRow(
          column(6,
            tags$label("Mortality m")
          ),
          column(6,
            sliderInput("scaleM", NULL, min = 5, max = 100, value = 40)
          )
        ),
        fluidRow(
          column(6,
            tags$label("Predators at the start")
          ),
          column(6,
            numericInput("spinnerP0", NULL, value = 20, min = 1, max = 500)
          )
        )
      )
    )
  ),
  fluidRow(
    column(6,
      tags$label("Years")
    ),
    column(6,
      numericInput("spinnerYears", NULL, value = 100, min = 10, max = 500)
    )
  ),
  fluidRow(
    column(12,
      checkboxInput("chkEquilibrium", "Show the equilibrium", value = TRUE)
    )
  ),
  fluidRow(
    column(12,
      tabsetPanel(id = "tabFolder",
        tabPanel("Time series",
          plotOutput("canvasSeries")
        ),
        tabPanel("Phase plane",
          plotOutput("canvasPhase")
        )
      )
    )
  ),
  fluidRow(
    column(12,
      textOutput("outSummary")
    )
  ),
  fluidRow(
    column(12,
      tableOutput("tableEquilibrium")
    )
  )
)
