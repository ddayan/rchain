package coop.rchain.syntax.rholang.Absyn; // Java Package generated by the BNF Converter.

public class QFalse extends RhoBool {
  public QFalse() { }

  public <R,A> R accept(coop.rchain.syntax.rholang.Absyn.RhoBool.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof coop.rchain.syntax.rholang.Absyn.QFalse) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return 37;
  }


}