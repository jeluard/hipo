(ns hipo.hipo-test
  (:require [hipo.compiler :refer [compile-set-attribute!]]))

; defmacro

(defmethod compile-set-attribute! "test2"
  [[el a v]]
  `(.setAttribute ~el ~a ~(* 2 v)))
