# The user interface of the Shiny app, created by Bio7 (WindowBuilder Shiny)
# from the GUI class AllWidgetsGui.java (/Bio7_Java_Examples/src/gui/windowbuilder).
# Change the GUI with the WindowBuilder and create the app again: this file is
# written again (the code of server.R stays).
library(shiny)

fluidPage(
  titlePanel("All Widgets (WindowBuilder Shiny)"),
  fluidRow(
    column(12,
      tags$label("All widgets which WindowBuilder Shiny and WindowBuilder JavaScript convert. Each label: the SWT widget -> Shiny / HTML. The values of all inputs are shown in the tab Outputs.")
    )
  ),
  fluidRow(
    column(6,
      actionButton("btnPress", "Button (push) -> actionButton / <button>")
    ),
    column(6,
      textOutput("outPressed")
    )
  ),
  fluidRow(
    column(12,
      tabsetPanel(id = "tabFolder",
        tabPanel("Inputs",
          div(
            fluidRow(
              column(6,
                tags$label("Scale -> sliderInput / <input type=range>")
              ),
              column(6,
                sliderInput("scaleValue", NULL, min = 0, max = 100, value = 50)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Slider -> sliderInput / <input type=range>")
              ),
              column(6,
                sliderInput("sliderValue", NULL, min = 0, max = 200, value = 30, step = 5)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Spinner -> numericInput / <input type=number>")
              ),
              column(6,
                numericInput("spinnerCount", NULL, value = 10, min = 0, max = 100)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Spinner with digits (2) -> decimals")
              ),
              column(6,
                numericInput("spinnerRate", NULL, value = 1.25, min = 0, max = 10, step = 0.01)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Text -> textInput / <input type=text>")
              ),
              column(6,
                textInput("txtName", NULL, value = "Bio7")
              )
            ),
            fluidRow(
              column(6,
                tags$label("Text (SWT.MULTI) -> textAreaInput / <textarea>")
              ),
              column(6,
                textAreaInput("txtNotes", NULL, placeholder = "Several lines")
              )
            ),
            fluidRow(
              column(6,
                tags$label("Text (SWT.PASSWORD) -> passwordInput / <input type=password>")
              ),
              column(6,
                passwordInput("txtSecret", NULL)
              )
            ),
            fluidRow(
              column(6,
                tags$label("DateTime (SWT.DATE) -> dateInput / <input type=date>")
              ),
              column(6,
                dateInput("dateDay", NULL)
              )
            ),
            fluidRow(
              column(6,
                tags$label("DateTime (SWT.TIME) -> textInput / <input type=time>")
              ),
              column(6,
                textInput("timeOfDay", NULL, placeholder = "hh:mm")
              )
            )
          )
        ),
        tabPanel("Choices",
          div(
            fluidRow(
              column(6,
                tags$label("Combo -> selectInput / <select>")
              ),
              column(6,
                selectInput("comboColor", NULL, choices = c("Blue", "Red", "Green"), selected = "Blue")
              )
            ),
            fluidRow(
              column(6,
                tags$label("List -> selectInput (a list) / <select size>")
              ),
              column(6,
                selectInput("listSpecies", NULL, choices = c("Fox", "Rabbit", "Deer", "Owl"), selectize = FALSE, size = 4)
              )
            ),
            fluidRow(
              column(6,
                tags$label("List (SWT.MULTI) -> selectInput(multiple) / <select multiple>")
              ),
              column(6,
                selectInput("listHabitats", NULL, choices = c("Forest", "Meadow", "Lake", "City"), multiple = TRUE, selectize = FALSE, size = 4)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Button (SWT.CHECK) -> checkboxInput / <input type=checkbox>")
              ),
              column(6,
                checkboxInput("chkActive", "Active", value = TRUE)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Button (SWT.TOGGLE) -> checkboxInput / <input type=checkbox>")
              ),
              column(6,
                checkboxInput("tglMode", "Expert mode", value = FALSE)
              )
            ),
            fluidRow(
              column(6,
                tags$label("Button (SWT.RADIO) in a Group -> radioButtons / <input type=radio>")
              ),
              column(6,
                wellPanel(
                  h4("Group -> wellPanel / <fieldset>"),
                  flowLayout(
                    radioButtons("grpSize", NULL, choices = c("Small", "Medium", "Large"), selected = "Medium")
                  )
                )
              )
            )
          )
        ),
        tabPanel("Outputs",
          div(
            fluidRow(
              column(6,
                tags$label("Canvas -> plotOutput / <canvas>")
              ),
              column(6,
                plotOutput("canvasPlot")
              )
            ),
            fluidRow(
              column(6,
                tags$label("Table -> tableOutput / <table>")
              ),
              column(6,
                tableOutput("tableValues")
              )
            ),
            fluidRow(
              column(6,
                tags$label("Label outText -> textOutput / <span>")
              ),
              column(6,
                textOutput("outText")
              )
            ),
            fluidRow(
              column(6,
                tags$label("Text outValues -> verbatimTextOutput / <pre>")
              ),
              column(6,
                verbatimTextOutput("outValues")
              )
            ),
            fluidRow(
              column(6,
                tags$label("Browser -> uiOutput / <div> (HTML)")
              ),
              column(6,
                uiOutput("browserInfo")
              )
            )
          )
        ),
        tabPanel("Layouts",
          div(
            fluidRow(
              column(12,
                tags$label("GridLayout -> fluidRow and column / a CSS grid (all tabs use it; a span: the width of several columns)")
              )
            ),
            fluidRow(
              column(12,
                tags$label("FillLayout (SWT.HORIZONTAL) -> columns of equal width / a flex row:")
              )
            ),
            fluidRow(
              column(12,
                div(
                  fluidRow(
                    column(4,
                      tags$label("A")
                    ),
                    column(4,
                      tags$label("B")
                    ),
                    column(4,
                      tags$label("C")
                    )
                  )
                )
              )
            ),
            fluidRow(
              column(12,
                tags$label("RowLayout -> flowLayout / a flex row which wraps:")
              )
            ),
            fluidRow(
              column(12,
                div(
                  flowLayout(
                    checkboxInput("chkOne", "One", value = FALSE),
                    checkboxInput("chkTwo", "Two", value = FALSE)
                  )
                )
              )
            ),
            fluidRow(
              column(12,
                tags$label("SashForm (SWT.HORIZONTAL) -> columns / a flex row:")
              )
            ),
            fluidRow(
              column(12,
                div(
                  fluidRow(
                    column(6,
                      tags$label("Left part")
                    ),
                    column(6,
                      tags$label("Right part")
                    )
                  )
                )
              )
            ),
            fluidRow(
              column(12,
                tags$label("CTabFolder (and TabFolder) -> tabsetPanel / tabs:")
              )
            ),
            fluidRow(
              column(12,
                tabsetPanel(id = "ctabFolder",
                  tabPanel("First",
                    tags$label("The content of the first tab")
                  ),
                  tabPanel("Second",
                    tags$label("The content of the second tab")
                  )
                )
              )
            ),
            fluidRow(
              column(12,
                HTML("Link -> HTML: <a href=\"https://bio7.org\">bio7.org</a>")
              )
            )
          )
        )
      )
    )
  )
)
