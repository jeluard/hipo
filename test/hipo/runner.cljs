(ns hipo.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [hipo.attribute-test]
              [hipo.compiler-test]
              [hipo.core-test]
              [hipo.hiccup-test]
              [hipo.interpreter-test]
              [hipo.reconciliation-test]))

(doo-tests 'hipo.attribute-test
           'hipo.compiler-test
           'hipo.core-test
           'hipo.hiccup-test
           'hipo.interpreter-test
           'hipo.reconciliation-test)
