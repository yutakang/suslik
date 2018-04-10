package org.tygus.synsl.logic

import org.tygus.synsl.SynSLException
import org.tygus.synsl.language.Expressions._

/**
  * Utilities for pure formulae
  *
  * @author Nadia Polikarpova, Ilya Sergey
  */
trait PureLogicUtils {

  def simplify(phi: PFormula): PFormula = phi match {
    case p@(PTrue | PFalse) => p
    case p@PLeq(left, right) => p // TODO: Improve this
    case p@PLtn(left, right) => p // TODO: Improve this
    case p@PEq(e, v@Var(_)) if !e.isInstanceOf[Var] => PEq(v, e)
    case p@PEq(v1@Var(n1), v2@Var(n2)) => // sort arguments lexicographically
      if (n1.toString <= n2.toString) PEq(v1, v2) else PEq(v2, v1)
    case p@PEq(_, _) => p

    //  Truth table for PAnd
    case PAnd(PFalse, right) => PFalse
    case PAnd(left, PFalse) => PFalse
    case PAnd(PTrue, right) => right
    case PAnd(left, PTrue) => left

    //  Truth table for POr
    case POr(PFalse, right) => right
    case POr(left, PFalse) => left
    case POr(PTrue, right) => PTrue
    case POr(left, PTrue) => PTrue

    //  The recursive cases
    case PAnd(left, right) => PAnd(simplify(left), simplify(right))
    case POr(left, right) => POr(simplify(left), simplify(right))

    //  Classical logic stuff and de Morgan
    case PNeg(PNeg(arg)) => simplify(arg)
    case PNeg(PAnd(left, right)) => POr(simplify(PNeg(left)), simplify(PNeg(right)))
    case PNeg(POr(left, right)) => PAnd(simplify(PNeg(left)), simplify(PNeg(right)))
    case PNeg(PTrue) => PFalse
    case PNeg(PFalse) => PTrue
    case PNeg(arg) => PNeg(simplify(arg))
  }

  private def isAtomicExpr(e: Expr): Boolean = e match {
    case Var(name) => true
    //  For now we only allow integers here
    case IntConst(_) => true
    case _ => false
  }

  val isAtomicPFormula: (PFormula) => Boolean = {
    case PTrue | PFalse => true
    case PEq(e1, e2) => isAtomicExpr(e1) && isAtomicExpr(e2)
    case PNeg(PEq(e1, e2)) => isAtomicExpr(e1) && isAtomicExpr(e2)
    case _ => false
  }

  def isCNF(isAtom: PFormula => Boolean)(pf: PFormula): Boolean = {
    def check(phi: PFormula): Boolean = phi match {
      case PLeq(_, _) | PLtn(_, _) | POr(_, _) => false
      case PAnd(left, right) => check(left) && check(right)
      case p => isAtom(p)
    }

    check(simplify(pf))
  }

  def conjuncts(phi: PFormula): Option[List[PFormula]] = {

    val pf = simplify(phi)
    if (!isCNF(isAtomicPFormula)(pf)) {
      throw PureLogicException(s"The formula ${phi.pp} is not in CNF")
    }

    def _conjuncts(p: PFormula): List[PFormula] = p match {
      case PTrue => Nil
      case atom if isAtomicPFormula(atom) => List(atom)
      case PAnd(left, right) => _conjuncts(left) ++ _conjuncts(right)
      case x => throw PureLogicException(s"Not a conjunction or an atomic pure formula: ${x.pp}")
    }

    Some(_conjuncts(pf).distinct)
  }

  def findCommon[T](cond: T => Boolean, ps1: List[T], ps2: List[T]): Option[(T, List[T], List[T])] = {
    for (p <- ps1 if cond(p)) {
      if (ps2.contains(p)) {
        return Some((p, ps1.filter(_ != p), ps2.filter(_ != p)))
      }
    }
    None
  }


  def isEquiv(p1: PFormula, p2: PFormula): Boolean = (p1, p2) match {
    case (PEq(e1, e2), PEq(e3, e4)) => e1 == e4 && e2 == e3
    case (PNeg(z1), PNeg(z2)) => isEquiv(z1, z2)
    case _ => p1 == p2
  }

  def findConjunctAndRest(p: PFormula => Boolean, phi: PFormula): Option[(PFormula, List[(PFormula)])] =
    conjuncts(phi).flatMap(cs => cs.find(p) match {
      case Some(c) => Some((c, cs.filter(e => e != c)))
      case None => None
    })

  def mkConjunction(ps: List[PFormula]): PFormula = ps.distinct match {
    case h :: t => t.foldLeft(h)((z, p) => PAnd(z, p))
    case Nil => PTrue
  }

}

case class PureLogicException(msg: String) extends SynSLException("pure", msg)