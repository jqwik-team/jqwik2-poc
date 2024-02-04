- From Atom to Choice:
    - List -> Sequence, Serialization letters
    - Can the exhaustive sources be replaced by  growing sources?
  
- JqwikProperty
    - tries = 0 && maxDuration = 0 -> run forever (until interrupted, falsified or genSource exhausted)
    - Allow suppression of duplicate samples
    - Report all falsified samples, checks, tries, effective seed
    - AfterFailureMode.SAMPLES_THEN_REPLAY
    - AfterFailureMode.ANALYZE: Find as many failing samples as possible and
      analyze their commonalities

- Target-based generation and shrinking
    - see http://proper.softlab.ntua.gr/Publications.html
    - simulated annealing: https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman

- Allow JqwikDefaults to be changed programmatically

- Additional generators and arbitraries
    - String (based on unicode code points)
    - Values.oneOf(...)
    - Values.frequencyOf(...)
    - Empty / Null Arbitrary -> When used in combinations, nothing is generated

- Add descriptions to generators
    - Can target type of map / flatMap be inferred for description?
      E.g. by generating a single sample and using its type?

- PropertyCase Execution
    - Fail if ratio checks/tries is too low

- Stateful generators
  - ActionChain?
  - Model-based testing?

- Statistics
    - Collect and report statistics
    - Generate until a statistical measurement is supposed to be stable.
      - See   https://www.youtube.com/watch?v=NcJOiQlzlXQ&list=PLvL2NEhYV4ZvCRCVlXTfB6-d09K3r0Sxa
      - See https://projecteuclid.org/journals/annals-of-mathematical-statistics/volume-16/issue-2/Sequential-Tests-of-Statistical-Hypotheses/10.1214/aoms/1177731118.full
      - https://en.wikipedia.org/wiki/Sequential_probability_ratio_test
      - Python implementation: https://github.com/Testispuncher/Sequential-Probability-Ratio-Test

- Shrinking (https://www.drmaciver.com/2019/01/notes-on-test-case-reduction/)
    - Compare shrinkables due to full length if different recording types are
      compared
    - Remove big chunks of elements when shrinking lists
    - Parallelize shrinking
        - https://github.com/DRMacIver/shrinkray/
        - https://dl.acm.org/doi/10.1145/3319619.3322004
    - Allow shrinking to use backup source when recording is exhausted
    - Cache tryable results during shrinking
    - Shrink with a timeout
    - Shrink in passes?
    - Show shrinking progress on console
    - Reduce list by improved binary
      search: https://notebook.drmaciver.com/posts/2019-04-30-13:03.html

- Jupiter Extension
