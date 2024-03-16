- New Core API
    - PropertyValidator.validateStatistically(minPercentage, standardDeviation)
    - Reporting
      - falsified samples
      - shrinking steps
      - run time
      - Do proper formatting in reports according to example-report.txt 
      - Make publisher interface feasible for use with JUnitPlatform reporting
    - Make afterFailure attribute multi value, e.g List.of(SAMPLES, REPLAY |
      RANDOMIZED)
        - PropertyAnalyzer: Find as many failing samples as possible and analyze
          their commonalities

- Additional generators and arbitraries
    - StringArbitrary: withChar(char... chars), withChar(CharSet chars),
      withCharRange(from, to)
    - Empty / Null Arbitrary -> When used in combinations, nothing is generated

- Target-based generation and shrinking
    - simulated
      annealing: https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman
    - see http://proper.softlab.ntua.gr/Publications.html

- Allow JqwikDefaults to be changed programmatically
    - Maybe Introduce Jqwik class and then Jqwik.defaults() and Jqwik.defaults(new JqwikDefaults())
  
- Add descriptions to generators
    - Can target type of map / flatMap be inferred for description?
      E.g. by generating a single sample and using its type?

- PropertyRun Execution
    - Fail if ratio checks/tries is too low

- Stateful generators
    - ActionChain?
    - Model-based testing?

- Statistics
    - Collect and report statistics
    - Replace ad-hoc approach for stability of classifiers with theoretical sound one:
        -
        See   https://www.youtube.com/watch?v=NcJOiQlzlXQ&list=PLvL2NEhYV4ZvCRCVlXTfB6-d09K3r0Sxa
        -
        See https://projecteuclid.org/journals/annals-of-mathematical-statistics/volume-16/issue-2/Sequential-Tests-of-Statistical-Hypotheses/10.1214/aoms/1177731118.full
        - https://en.wikipedia.org/wiki/Sequential_probability_ratio_test
        - Python
          implementation: https://github.com/Testispuncher/Sequential-Probability-Ratio-Test

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

- Misc
    - Loading samples that provide wrong number of gen sources -> IllegalArgumentException
      Should be ignored and sample be deleted