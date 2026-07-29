(ns association-facts-test
  (:require [clojure.java.io :as io] [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [kotoba.compiler.core :as compiler] [kotoba.compiler.ir :as ir]))
(def source (slurp "src/association_facts.kotoba"))
(defn call [kir f & xs] (ir/execute kir f (vec xs)))
(defn present [x] (when (second x) (nth x 2)))
(def fields ["id" "title" "association" "isic" "country" "kind" "url" "url-provenance"
             "established-date" "last-revised-date" "retrieved-at"
             "addressee" "parameter-disposition"])
(def expected
  [{"id" "aba.1985-restructure" "title" "1985 restructure -- formation of the modern ABA (The ABA)"
    "association" "aba" "isic" "6419" "country" "AUS" "kind" "governance-program"
    "url" "https://www.ausbanking.org.au/about-us/the-aba/" "url-provenance" "official-ausbanking-org-au"
    "established-date" "1985" "last-revised-date" nil "retrieved-at" "2026-07-16"
    "addressee" "member-bank" "parameter-disposition" "no-parameter"}
   {"id" "aba.1997-mission-refocus" "title" "1997 mission refocus toward advocacy (The ABA)"
    "association" "aba" "isic" "6419" "country" "AUS" "kind" "policy-position"
    "url" "https://www.ausbanking.org.au/about-us/the-aba/" "url-provenance" "official-ausbanking-org-au"
    "established-date" "1997" "last-revised-date" nil "retrieved-at" "2026-07-16"
    "addressee" "member-bank" "parameter-disposition" "no-parameter"}
   {"id" "aba.scam-safe-accord" "title" "Scam-Safe Accord (Keeping Australia Scam Safe)"
    "association" "aba" "isic" "6419" "country" "AUS" "kind" "self-regulatory-code"
    "url" "https://www.ausbanking.org.au/scam-safe-accord/" "url-provenance" "official-ausbanking-org-au"
    "established-date" nil "last-revised-date" nil "retrieved-at" "2026-07-29"
    "addressee" "member-bank" "parameter-disposition" "mandates-control-not-number"}])
(deftest reference-preserves-authority
  (let [kir (:kir (compiler/compile-source source :js-kotoba-v1))
        observed (mapv (fn [i] (into {} (map (fn [f] [f (present (call kir 'entry-field "aba" i f))]) fields))) [0 1 2])]
    (is (= expected observed))
    (is (= ["1985" "1997"] (mapv #(present (call kir 'entry-field "aba" % "established-date")) [0 1])))
    (is (= ["governance" "governance"] (mapv #(present (call kir 'topic "aba" % 0)) [0 1])))
    (is (= ["aba.1985-restructure" "aba.1997-mission-refocus"]
           (mapv #(present (call kir 'by-topic-id "aba" "governance" %)) [0 1])))
    (is (= #{} (set (:effects kir))))
    (testing "fail closed"
      (is (zero? (call kir 'entry-count "australian-banking-association")))
      (is (nil? (present (call kir 'entry-field "aba" 3 "id"))))
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
            (str "import(process.argv[1]).then(async h=>{const j=await import('data:text/javascript;base64," js64 "');const w=await h.instantiateKotoba(Buffer.from(process.argv[2],'base64'));const r=x=>{if(x['entry-field']('aba',0n,'established-date')[2]!=='1985'||x['entry-field']('aba',1n,'established-date')[2]!=='1997'||x['entry-field']('aba',0n,'last-revised-date')[1]!==false)throw Error('dates');if(x['by-topic-count']('aba','governance')!==2n||x['by-topic-id']('aba','governance',1n)[2]!=='aba.1997-mission-refocus'||x['entry-count']('australian-banking-association')!==0n)throw Error('authority');if(x['entry-count']('aba')!==3n||x['sets-numeric-default-count']('aba','transfer-limit')!==0n||x['disposition-count']('aba','transfer-limit','mandates-control-not-number')!==1n)throw Error('disposition');};r(j.instantiateKotoba({}));r(w.instance.exports)}).catch(e=>{console.error(e);process.exit(99)})")
            (.toString (.toUri (.resolve (compiler-root) "runtime/browser-host.mjs"))) wasm64)]
    (is (zero? (:exit p)) (str (:out p) (:err p)))))
(deftest production-source-authority
  (is (= ["src/association_facts.kotoba"] (->> (file-seq (io/file "src")) (filter #(.isFile %)) (map str) sort vec))))

(deftest the-accord-mandates-a-control-not-a-number
  ;; The comparative finding this field exists to support. The Scam-Safe
  ;; Accord obliges MEMBER BANKS to ship controls -- name-checking
  ;; (Confirmation of Payee), identity verification, intelligence sharing,
  ;; limits on high-risk payment channels -- and attaches no numeric
  ;; threshold to any of them (verified live 2026-07-29: the only numbers
  ;; on the page are loss and volume statistics).
  ;;
  ;; Contrast with cloud-itonami-assoc-6419-jpn-zenginkyo, where the
  ;; corporate transfer limit is named and the NUMBER is handed to the
  ;; customer, with compensation then conditioned on how the customer set
  ;; it. Neither association fixes a number. They differ in who is obliged
  ;; to do something instead.
  (let [kir (:kir (compiler/compile-source source :js-kotoba-v1))]
    (is (= 3 (call kir 'entry-count "aba")))
    (is (= 1 (call kir 'by-topic-count "aba" "transfer-limit")))
    (is (= 0 (call kir 'sets-numeric-default-count "aba" "transfer-limit")))
    (is (= 1 (call kir 'disposition-count "aba" "transfer-limit" "mandates-control-not-number")))
    (is (= 0 (call kir 'disposition-count "aba" "transfer-limit" "delegates-to-adopter")))
    (is (= "member-bank" (present (call kir 'entry-field "aba" 2 "addressee")))
        "the accord speaks to banks, not to customers")
    (is (nil? (present (call kir 'entry-field "aba" 2 "established-date")))
        "no date is shown on the page, so none is stored -- inferring one would be fabrication")
    (testing "no fabricated numeric default under any catalog topic"
      (doseq [t ["governance" "financial-crime" "transfer-limit"]]
        (is (= 0 (call kir 'sets-numeric-default-count "aba" t)) t)))))

(deftest counts-are-derived-not-restated
  (let [kir (:kir (compiler/compile-source source :js-kotoba-v1))
        topics ["governance" "financial-crime" "transfer-limit"]
        dispositions ["sets-numeric-default" "delegates-to-adopter"
                      "conditions-liability-on-adopter-choice"
                      "mandates-control-not-number" "no-parameter"]]
    (is (= 4 (reduce + (map #(call kir 'by-topic-count "aba" %) topics)))
        "two entries with one topic each plus one with two")
    (doseq [t topics]
      (is (= (call kir 'by-topic-count "aba" t)
             (reduce + (map #(call kir 'disposition-count "aba" t %) dispositions)))
          (str "every entry under " t " carries a disposition from the closed set")))))

(deftest datascript-tx-matches-kotoba-authority
  ;; `data/datascript-tx.edn` is documented as DERIVED from the Kotoba
  ;; catalog. Nothing regenerates it, so without this check the two could
  ;; drift and a downstream query would answer from a stale copy while the
  ;; tests passed against the real authority.
  (let [kir (:kir (compiler/compile-source source :js-kotoba-v1))
        tx (read-string (slurp "data/datascript-tx.edn"))
        kw #(when % (keyword %))
        from-kotoba
        (mapv (fn [i]
                (let [f #(present (call kir 'entry-field "aba" i %))]
                  (cond-> {:association-rule/id (f "id")
                           :association-rule/title (f "title")
                           :association-rule/association (f "association")
                           :association-rule/isic (f "isic")
                           :association-rule/country (f "country")
                           :association-rule/kind (kw (f "kind"))
                           :association-rule/url (f "url")
                           :association-rule/url-provenance (kw (f "url-provenance"))
                           :association-rule/retrieved-at (f "retrieved-at")
                           :association-rule/addressee (kw (f "addressee"))
                           :association-rule/parameter-disposition (kw (f "parameter-disposition"))
                           :association-rule/topic (mapv #(keyword (present (call kir 'topic "aba" i %)))
                                                         (range (call kir 'topic-count "aba" i)))}
                    (f "established-date") (assoc :association-rule/established-date (f "established-date"))
                    (f "last-revised-date") (assoc :association-rule/last-revised-date (f "last-revised-date")))))
              [0 1 2])]
    (is (= (count from-kotoba) (count tx)))
    (is (= from-kotoba (mapv #(into {} %) tx)))))
