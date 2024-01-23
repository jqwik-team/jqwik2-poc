- Refactoring
    - Replace GenSource.Tree with GenSource.Tuple
  
- JqwikProperty
    - Report all falsified samples, checks, tries, effective seed
    - AfterFailureMode.SAMPLES_THEN_REPLAY
    - AfterFailureMode.ANALYZE: Find as many failing samples as possible and analyze their commonalities

- Allow JqwikDefaults to be changed programmatically

- Introduce Arbitraries
    - flatMap

- Additional generators
    - String
    - Empty / Null Generator

- Add descriptions to generators
    - Can target type of map / flatMap be inferred for description?
      E.g. by generating a single sample and using its type?

- PropertyCase Execution
    - Fail if ratio checks/tries is too low
- Add GenSource.Tuple
- Stateful generators
- Statistics
    - Collect and report statistics
    - Generate until a statistical measurement is supposed to be stable.
      See   https://www.youtube.com/watch?v=NcJOiQlzlXQ&list=PLvL2NEhYV4ZvCRCVlXTfB6-d09K3r0Sxa

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
- Target-based generation and shrinking
- Give generators equality to allow caching (Is that still necessary?)
