(ns edl.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [edl.schema.public :as pub]))

;; Initialize the database with the file testdb.sql before running
;; any test. The way things work at compile time, you cannot create
;; fixtures for this.

;; We can use fixtures to test updates though.
(defn skywalker-record
  [f]
  (j/insert! pub/db :users {:id 3 :name "Luke Skywalker" :email "skywalker@rebels.tt"})
  (f)
  (j/delete! pub/db :users ["id=3"]))

(use-fixtures :each skywalker-record)

(deftest retrieval
  (testing "get-record"
    (is (= (pub/get-record pub/db :users 1)
           {:email "mary@domain.com", :name "Mary Smith", :id 1}))
    (is (= (pub/get-record pub/db :users 1 [:email :name])
           {:email "mary@domain.com", :name "Mary Smith"}))
    )
  (testing "get-field"
    (is (= (pub/get-field pub/db :users 2 :email)
           "john@domain.com"))))

(deftest update-record
  (testing "update-record"
    (let [updated (pub/update-record! pub/db :users 3 {:name "Han Solo" :email "solo@rebels.tt"})]
      (is (= 1 updated))
      (is (= (pub/get-record pub/db :users 3) {:id 3 :name "Han Solo" :email "solo@rebels.tt"})))))

(deftest table-map
  (testing "table-maps"
    (is (= pub/categories {:biology 3, :math 2, :computer-science 1}))))

(comment
  ;; These  cannot be tested in a traditional way as they trigger
  ;; compilation errors.

  ;; Misspelled the users table.
  (pub/get-record pub/db :usrs 1)
  (pub/get-record pub/db :uers 1 [:name :email :phone])

  ;; Misspelled a column.
  (pub/get-record pub/db :users 1 :emil)

  ;; Column doesn't exist.
  (pub/get-record pub/db :users 1 [:name :email :phone])

  ;; Column wrong.
  (pub/update-record! pub/db :users 3 {:name "Han Solo" :emal "solo@rebels.tt"})

  )