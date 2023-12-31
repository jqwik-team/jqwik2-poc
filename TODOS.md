- Additional generators
    - Generator.flatMap()
    - Set of values
    - String
- Introduce Arbitraries
- Add descriptions to generators
- Give generators equality to allow caching
- Introduce Jqwik or JqwikSession object as public entry point
- PropertyCase Execution
    - Fail if ratio checks/tries is too low
    - Save seed and falsified samples to disk (.jqwik folder)
    - Allow rerun with previously falsified samples (starting with smallest)
      => Rerun will improve shrinking!
    - Allow execution with exhaustive generation
- Exhaustive generation
    - Make maxCount() calculation stable against overflow
- Add GenSource.Tuple
- Stateful generators
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
- Jupiter Extension
- Target-based generation and shrinking
