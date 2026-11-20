# The user interface of the Shiny app, created by Bio7 (WindowBuilder Shiny)
# from the GUI class FoodChainGlGui.java (/Bio7_Java_Examples/src/wbgl).
# Change the GUI with the WindowBuilder and create the app again: this file is
# written again (the code of server.R stays).
library(shiny)

fluidPage(
  titlePanel("Food Chain in 3D (Shiny with rgl)"),
  fluidRow(
    column(6,
      tags$label("Half saturation b1 (1/10)")
    ),
    column(6,
      sliderInput("scaleB1", NULL, min = 20, max = 62, value = 30)
    )
  ),
  fluidRow(
    column(6,
      tags$label("Time")
    ),
    column(6,
      numericInput("spinnerTime", NULL, value = 5000, min = 500, max = 20000, step = 500)
    )
  ),
  fluidRow(
    column(6,
      tags$label("Transient left out (%)")
    ),
    column(6,
      sliderInput("scaleTransient", NULL, min = 0, max = 80, value = 20)
    )
  ),
  fluidRow(
    column(6,
      tags$label("Color of the curve")
    ),
    column(6,
      selectInput("comboColor", NULL, choices = c("Time", "Top predator"), selected = "Time")
    )
  ),
  fluidRow(
    column(12,
      actionButton("btnRun", "Run")
    )
  ),
  fluidRow(
    column(6,
      rgl::rglwidgetOutput("glAttractor")
    ),
    column(6,
      plotOutput("canvasSeries")
    )
  ),
  fluidRow(
    column(12,
      textOutput("outSummary")
    )
  )
)
