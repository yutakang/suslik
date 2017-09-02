package org.tygus.synsl.logic

import org.tygus.synsl.{PrettyPrinting, Substitutable}
import org.tygus.synsl.language.{IntType, SynslType}
import org.tygus.synsl.language.Expressions._

object Specifications extends SpatialFormulas {

  type Gamma = Seq[(SynslType, Var)]

  case class Assertion(phi: PureFormula, sigma: SFormula) extends Substitutable[Assertion] {

    def pp: String = s"{${phi.pp} ; ${sigma.canonicalize(_ => true).pp}}"

    // Get free variables
    def varsPhi: Set[Var] = phi.collectE(_.isInstanceOf[Var])
    def varsSigma: Set[Var] = sigma.collectE(_.isInstanceOf[Var])

    // Get all variables in the assertion
    def vars: Set[Var] = varsPhi ++ varsSigma

    // Collect arbitrary expressions
    def collectE[R <: Expr](p: Expr => Boolean): Set[R] =
      phi.collectE(p) ++ sigma.collectE(p)

    def subst(x: Var, by: Expr): Assertion = Assertion(phi.subst(x, by), sigma.subst(x, by))

    def removeHeaplet(f: PointsTo => Boolean) = Assertion(phi, sigma.removeHeaplet(f))
  }


  /**
    * Main class for contextual Hoare-style specifications
    */
  case class Spec(pre: Assertion, post: Assertion, gamma: Gamma) extends PrettyPrinting {

    override def pp: String =
      s"${gamma.map { case (t, i) => s"${t.pp} ${i.pp}" }.mkString(", ")} |- " +
          s"${pre.pp} ${post.pp}"

    def vars: Set[Var] = pre.vars ++ post.vars ++ gamma.map(_._2)

    def ghosts: Set[Var] = pre.vars -- formals

    def formals: Set[Var] = gamma.map(_._2).toSet

    def existentials: Set[Var] = (post.vars -- ghosts) -- formals

    def givenConstants: Set[PConst] = pre.collectE(_.isInstanceOf[PConst])

    def constantsInPost: Set[PConst] = post.collectE(_.isInstanceOf[PConst])

    // Determine whether `x` is a ghost vriaable wrt. given spec and gamma
    def isGhost(x: Var): Boolean = ghosts.contains(x)

    // Determine whether x is in the context
    def isConcrete(x: Var) : Boolean = gamma.map(_._2).contains(x)

    def getType(x: Var): Option[SynslType] = {
      // TODO: all ghosts are ints for now, until we descide how to infer it
      if (isGhost(x)) return Some(IntType)

      // Typed variables get the type automatically
      gamma.find(_._2 == x) match {
        case Some((t, _)) => Some(t)
        case None => None
      }
    }
  }

  /**
    * Hoare-style SL specification
    *
    * @param spec pre/postconditions and variable context
    * @param name Procedure name (optional)
    * @param tpe  Procedure return type
    */
  case class FullSpec(spec: Spec, tpe: SynslType, name: Option[Ident])
      extends PrettyPrinting {

    override def pp: String = {
      val Spec(pre, post, gamma) = spec
      s"${pre.pp}\n${tpe.pp} " +
          s"$name(${gamma.map { case (t, i) => s"${t.pp} ${i.pp}" }.mkString(", ")})\n" +
          s"${post.pp}"
    }

  }


}