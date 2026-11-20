# The server of the widget example (WindowBuilder Shiny; the design:
# src/wbwidgets/AllWidgetsGui.java, ui.R is created from it). Bio7 writes this file
# only once: this code stays when ui.R is created again.
#
# Each widget of the design is an input or an output of Shiny with the name of its
# variable: the inputs are read with input$<name>, the outputs written with
# output$<name>. The tab Outputs shows the values of all inputs (and their R class:
# e.g., a slider gives a number, a date input a Date, a multiple list a vector).
library(shiny)

function(input, output, session) {

  # The ids of the inputs of the design (the names of the variables of the GUI class)
  ids <- c("scaleValue", "sliderValue", "spinnerCount", "spinnerRate", "txtName", "txtNotes", "txtSecret",
           "dateDay", "timeOfDay", "comboColor", "listSpecies", "listHabitats", "chkActive", "tglMode",
           "grpSize", "chkOne", "chkTwo", "tabFolder", "ctabFolder")

  values <- reactive({
    v <- lapply(ids, function(id) input[[id]])
    names(v) <- ids
    v
  })

  show <- function(x) if (is.null(x) || length(x) == 0) "(none)" else paste(format(x), collapse = ", ")

  # Button (push): actionButton, its value counts the clicks
  output$outPressed <- renderText(sprintf("The button was pressed %d times.", input$btnPress))

  # Canvas: plotOutput (renderPlot). The scale sets the number of values, the combo the color
  output$canvasPlot <- renderPlot({
    n <- 10 + 10 * input$scaleValue
    hist(rnorm(n), col = tolower(input$comboColor), border = "white",
         main = sprintf("%d normal values (Scale), color of the Combo", n), xlab = "x")
  })

  # Table: tableOutput (renderTable) - all inputs, their values and R classes
  output$tableValues <- renderTable({
    v <- values()
    data.frame(Input = paste0("input$", names(v)), Value = vapply(v, show, ""),
               Class = vapply(v, function(x) if (is.null(x)) "NULL" else class(x)[1], ""),
               check.names = FALSE)
  })

  # Label "out...": textOutput (renderText)
  output$outText <- renderText(sprintf("Hello %s: color %s, size %s%s.", input$txtName, input$comboColor, input$grpSize,
                                       if (isTRUE(input$chkActive)) ", active" else ""))

  # Text "out...": verbatimTextOutput (renderPrint)
  output$outValues <- renderPrint(str(values()))

  # Browser: uiOutput (renderUI) - HTML of the server
  output$browserInfo <- renderUI(tags$div(tags$b("HTML of the server: "),
                                          tags$span(style = paste0("color:", tolower(input$comboColor)), input$txtName)))
}
