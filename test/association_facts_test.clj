(ns association-facts-test
  (:require [clojure.java.io :as io] [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [kotoba.compiler.core :as compiler] [kotoba.compiler.ir :as ir]))
(def source (slurp "src/association_facts.kotoba"))
(defn call [kir f & xs] (ir/execute kir f (vec xs)))
(defn present [x] (when (second x) (nth x 2)))
(def fields ["id" "title" "association" "isic" "country" "kind" "url" "url-provenance"
             "established-date" "last-revised-date" "retrieved-at"])
(def expected
  [{"id" "aba.1985-restructure" "title" "1985 restructure -- formation of the modern ABA (The ABA)"
    "association" "aba" "isic" "6419" "country" "AUS" "kind" "governance-program"
    "url" "https://www.ausbanking.org.au/about-us/the-aba/" "url-provenance" "official-ausbanking-org-au"
    "established-date" "1985" "last-revised-date" nil "retrieved-at" "2026-07-16"}
   {"id" "aba.1997-mission-refocus" "title" "1997 mission refocus toward advocacy (The ABA)"
    "association" "aba" "isic" "6419" "country" "AUS" "kind" "policy-position"
    "url" "https://www.ausbanking.org.au/about-us/the-aba/" "url-provenance" "official-ausbanking-org-au"
    "established-date" "1997" "last-revised-date" nil "retrieved-at" "2026-07-16"}])
(deftest reference-preserves-authority
  (let [kir (:kir (compiler/compile-source source :js-kotoba-v1))
        observed (mapv (fn [i] (into {} (map (fn [f] [f (present (call kir 'entry-field "aba" i f))]) fields))) [0 1])]
    (is (= expected observed))
    (is (= ["1985" "1997"] (mapv #(present (call kir 'entry-field "aba" % "established-date")) [0 1])))
    (is (= ["governance" "governance"] (mapv #(present (call kir 'topic "aba" % 0)) [0 1])))
    (is (= ["aba.1985-restructure" "aba.1997-mission-refocus"]
           (mapv #(present (call kir 'by-topic-id "aba" "governance" %)) [0 1])))
    (is (= #{} (set (:effects kir))))
    (testing "fail closed"
      (is (zero? (call kir 'entry-count "australian-banking-association")))
      (is (nil? (present (call kir 'entry-field "aba" 2 "id"))))
      (is (nil? (present (call kir 'entry-field "aba" 0 "last-revised-date"))))
      (is (nil? (present (call kir 'topic "aba" 0 1))))
      (is (zero? (call kir 'by-topic-count "aba" "founding")))
      (is (nil? (present (call kir 'by-topic-id "aba" "governance" 2)))))))
(defn compiler-root [] (nth (iterate #(.getParent ^java.nio.file.Path %)
  (java.nio.file.Path/of (.toURI (io/resource "kotoba/compiler/core.clj")))) 4))
(defn base64 [x] (.encodeToString (java.util.Base64/getEncoder) x))
(deftest restricted-js-and-wasm-conform-semantically
  (let [js (compiler/compile-source source :js-kotoba-v1) wasm (compiler/compile-source source :wasm32-browser-kotoba-v1)
        js64 (base64 (.getBytes ^String (:source js) "UTF-8")) wasm64 (base64 ^bytes (:bytes wasm))
        p (shell/sh "node" "--input-type=module" "-e"
            (str "import(process.argv[1]).then(async h=>{const j=await import('data:text/javascript;base64," js64 "');const w=await h.instantiateKotoba(Buffer.from(process.argv[2],'base64'));const r=x=>{if(x['entry-field']('aba',0n,'established-date')[2]!=='1985'||x['entry-field']('aba',1n,'established-date')[2]!=='1997'||x['entry-field']('aba',0n,'last-revised-date')[1]!==false)throw Error('dates');if(x['by-topic-count']('aba','governance')!==2n||x['by-topic-id']('aba','governance',1n)[2]!=='aba.1997-mission-refocus'||x['entry-count']('australian-banking-association')!==0n)throw Error('authority');};r(j.instantiateKotoba({}));r(w.instance.exports)}).catch(e=>{console.error(e);process.exit(99)})")
            (.toString (.toUri (.resolve (compiler-root) "runtime/browser-host.mjs"))) wasm64)]
    (is (zero? (:exit p)) (str (:out p) (:err p)))))
(deftest production-source-authority
  (is (= ["src/association_facts.kotoba"] (->> (file-seq (io/file "src")) (filter #(.isFile %)) (map str) sort vec))))
