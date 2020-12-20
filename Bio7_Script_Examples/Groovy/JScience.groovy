/*
This Snippet demonstrates the use of the JScience API with Groovy!
*/


import org.jscience.mathematics.number.Complex
import static org.jscience.mathematics.number.Complex.I
import static org.jscience.mathematics.number.Complex.valueOf as c
import org.jscience.mathematics.function.Polynomial
import static org.jscience.mathematics.function.Polynomial.valueOf as p
import org.jscience.mathematics.function.Variable

// Defines two local variables (x, y).
def varX = new Variable.Local<Complex>("x")
def varY = new Variable.Local<Complex>("y")

use (JScienceCategory) {
    def ONE = Complex.ONE
    def TWO = c(2, 0)
    // f(x) = ix² + 2x + 1
    def x = p(ONE, varX)
    def fx = I * x ** 2 + TWO * x + ONE
    println fx
    println fx ** 2
    println fx.differentiate(varX)
    println fx.integrate(varY)
    println fx.compose(fx)

    // Calculates expression.
    varX.set(c(2, 3))
    println fx.evaluate()
}

class JScienceCategory {
    static power(Polynomial p, int n) {
        p.pow(n)
    }
    static multiply(Complex c, Polynomial p) {
        p.times(c)
    }
    static multiply(Polynomial p, Complex c) {
        p.times(c)
    }
}