package org.tygus.suslik.synthesis

import org.scalatest.{FunSpec, Matchers}
import org.tygus.suslik.synthesis.instances.PhasedSynthesis

class CardioTests extends FunSpec with Matchers with SynthesisRunnerUtil {

  val synthesis: Synthesis = new PhasedSynthesis

  override def doRun(testName: String, desc: String, in: String, out: String, params: SynConfig = defaultConfig): Unit = {
    super.doRun(testName, desc, in, out, params)
    it(desc) {
      synthesizeFromSpec(testName, in, out, params)
    }
  }

  describe("SuSLik with Cardinalities should be able synthesize") {
    runAllTestsFromDir("cardio")
  }


}