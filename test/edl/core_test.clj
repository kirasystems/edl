(ns edl.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [edl.schema.public :as pub]))

;; Initialize the database with the file testdb.sql before running
;; any test. The way things work at compile time, you cannot create
;; fixtures for this.

(deftest a-test
  (testing "get-record"
    (is (= (pub/get-record pub/db :users 1)
           {:email "mary@domain.com", :name "Mary Smith", :id 1}))
    (is (= (pub/get-record pub/db :users 2 :email)
           "john@domain.com"))
    (is (= (pub/get-record pub/db :users 1 [:email :name])
           {:email "mary@domain.com", :name "Mary Smith"}))))

(comment
  ;; These demos cannot be tested in a traditional way as they trigger
  ;; compilation errors.

  ;; Misspelled the users table.
  (pub/get-record pub/db :usrs 1)

  ;; Misspelled a column.
  (pub/get-record pub/db :users 1 :emil)

  ;; Column doesn't exist.
  (pub/get-record pub/db :users 1 [:name :email :phone])
  )