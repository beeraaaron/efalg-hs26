package compgeo;

import java.math.BigInteger;

public final class Fraction implements Comparable<Fraction> {
    public static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
    public static final Fraction ONE  = new Fraction(BigInteger.ONE, BigInteger.ONE);

    private final BigInteger num;
    private final BigInteger den; // always > 0, fraction always reduced

    public Fraction(BigInteger num, BigInteger den) {
        if (den.signum() == 0) throw new ArithmeticException("Denominator is zero");
        if (den.signum() < 0) {
            num = num.negate();
            den = den.negate();
        }

        BigInteger g = num.gcd(den);
        if (g.signum() != 0 && !g.equals(BigInteger.ONE)) {
            num = num.divide(g);
            den = den.divide(g);
        }
        this.num = num;
        this.den = den;
    }

    public static Fraction of(long n, long d) {
        return new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    // Exact conversion from a decimal string like "0.1" -> 1/10
    public static Fraction parse(String decimal) {
        java.math.BigDecimal bd = new java.math.BigDecimal(decimal);
        BigInteger unscaled = bd.unscaledValue();
        int scale = bd.scale();
        return scale >= 0
                ? new Fraction(unscaled, BigInteger.TEN.pow(scale))
                : new Fraction(unscaled.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE);
    }

    public Fraction add(Fraction o) {
        return new Fraction(num.multiply(o.den).add(o.num.multiply(den)), den.multiply(o.den));
    }
    public Fraction subtract(Fraction o) {
        return new Fraction(num.multiply(o.den).subtract(o.num.multiply(den)), den.multiply(o.den));
    }
    public Fraction multiply(Fraction o) {
        return new Fraction(num.multiply(o.num), den.multiply(o.den));
    }
    public Fraction divide(Fraction o) {
        return new Fraction(num.multiply(o.den), den.multiply(o.num));
    }

    public boolean isZero() { return num.signum() == 0; }

    @Override public int compareTo(Fraction o) {
        return num.multiply(o.den).compareTo(o.num.multiply(den));
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof Fraction f && num.equals(f.num) && den.equals(f.den);
    }

    @Override public int hashCode() {
        return 31 * num.hashCode() + den.hashCode();
    }

    @Override public String toString() {
        return den.equals(BigInteger.ONE) ? num.toString() : num + "/" + den;
    }

    public double toDouble() {
        return new java.math.BigDecimal(num).divide(new java.math.BigDecimal(den), java.math.MathContext.DECIMAL64).doubleValue();
    }
}