package org.tygus.suslik.synthesis
import org.tygus.suslik.language.Expressions._
import org.tygus.suslik.language.SSLType
import org.tygus.suslik.language.Statements._
import org.tygus.suslik.logic.{Block, Heaplet, PFormula, PointsTo, SApp, SFormula}
import org.tygus.suslik.logic.Specifications.Assertion

import scala.collection.immutable.{SortedSet, TreeSet}
object Evaluator {
  case class EvalResult(result: Assertion,
                        store: Map[Var, Expr]
                       )
  type Heap = List[Heaplet]
  def evaluate(s: Statement, heap: Heap) : Heap ={
    s match {
      case Skip => return heap
      case Error => throw new RuntimeException("statement has an error in it to begin with")
      case SeqComp(s1,s2)=> return evaluate(s2, evaluate(s1,heap))
      case Load(to, _, from, offset) => {
        // to = b, from = x, offset = 1
        // lookup (from+offset) from Pre, giving PointsTo(x+1, v). (we know v = b, but doing this for completeness' sake)
        // then, want to replace every instance of (to = b) with (v = b).
        // do this by looking up "to" from Pre and replacing it with "v", giving PointsTo(to, _) :=> PointsTo(v, _)
        // PointsTo(_, to) :=> PointsTo(_, v); Block(to,_x`) :=> Block(v,_); Block(_, to) := Block(_,v)
        val curr = heap.collect{case PointsTo(loc, offset, value) => PointsTo(loc,offset,value)}.filter{
          case PointsTo(l,o,v) => from == l && offset == o
        }.head
        val curr_value = curr.value
        val new_heap = heap.map{
          case PointsTo(l,o,v) => if (l == to) {
            PointsTo(curr_value,o,v)
          } else if (v == to) {
            PointsTo(l,o, curr_value)
          } else {PointsTo(l,o,v)}
          case Block(l,p) => if (l == to) {
            Block(curr_value,p)
          }
        }.asInstanceOf[List[Heaplet]]
        return new_heap
      }
      case Free(v_name) =>
        val new_heap = heap.filter{
          case Block(name, i) => {
            name != v_name
          }
          case PointsTo(l,_,_) => l != v_name
        }
        return new_heap
      case Store(to,offset,e) =>
        val new_heap = heap.map{
          case PointsTo(l,o,v) => if (l == to & o == offset) {
            PointsTo(l,o,e)
          } else {PointsTo(l,o,v)}
          case Block(l, i) => Block(l,i)
        }.asInstanceOf[List[Heaplet]]
        return new_heap
      case Malloc(to: Var, tpe: SSLType, sz) => {
        val heap_with_allocated_block = Block(to, sz) :: heap
        var new_heap = heap_with_allocated_block
        for (i <- 0 until sz) {
          new_heap = PointsTo(to, i, IntConst(-1)):: new_heap
        }
        return new_heap
      }
      case _ => ???
    }

  }

}
