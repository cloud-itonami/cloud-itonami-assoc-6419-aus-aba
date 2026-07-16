(ns association.facts
  "Industry rule/history catalog for the Australian Banking Association
  (ABA) -- a 32nd industry-association-level source per ADR-2607141700
  (cloud-itonami-compliance-fact-federation). The FOURTH entry aligned
  to ISIC 6419 (other monetary intermediation / banking), alongside
  cloud-itonami-assoc-6419-jpn-zenginkyo (Japan), -6419-deu-bankenverband
  (Germany), and -6419-fra-fbf (France) -- the same
  cross-country-same-ISIC pattern already used for ISIC 2910
  (VDA/SMMT).

  NASSCOM (nasscom.in) was attempted first this tick, but its entire
  domain returned HTTP 406 on every URL tried (root page included) --
  abandoned outright, a new whole-domain-block failure class this
  session. The Canadian Bankers Association (cba.ca) was attempted
  next, but its History and Milestones pages both returned unresolvable
  HTTP 307 redirects, and an alternate PDF mirror (a public inquiry
  commission's institutional report) rendered almost entirely
  illegible via font-subsetting -- also abandoned.

  Both entries here instead cite ABA's own directly WebFetch-verified
  'The ABA' history page (ausbanking.org.au/about-us/the-aba/), which
  states plainly that the organisation 'traces its history back to
  the late 1940s' (no exact founding year given by the org itself,
  deliberately not guessed here) but directly describes two dated
  restructuring milestones: the 1985 merger of three prior
  organisations (Australian Banking Association-Research Directorate,
  Australian Banking Association, and Banking Education Service, plus
  integration of the Banks' Industrial Association) that formed the
  modern ABA and a new constitution; and the 1997 mission refocus
  toward advocacy ('an advocate for the banking industry when dealing
  with Governments, the media and public'). Both dates are year-only,
  matching the year-only-date discipline already used for several
  sibling association entries.

  A rule not in this table has NO spec-basis, full stop; extend
  `catalog`, do not invent an id/url.")

(def catalog
  "assoc-slug -> vector of self-regulatory rule entries."
  {"aba"
   [{:association-rule/id "aba.1985-restructure"
     :association-rule/title "1985 restructure -- formation of the modern ABA (The ABA)"
     :association-rule/association "aba"
     :association-rule/isic "6419"
     :association-rule/country "AUS"
     :association-rule/kind :governance-program
     :association-rule/url "https://www.ausbanking.org.au/about-us/the-aba/"
     :association-rule/url-provenance :official-ausbanking-org-au
     :association-rule/established-date "1985"
     :association-rule/retrieved-at "2026-07-16"
     :association-rule/topic #{:governance}}
    {:association-rule/id "aba.1997-mission-refocus"
     :association-rule/title "1997 mission refocus toward advocacy (The ABA)"
     :association-rule/association "aba"
     :association-rule/isic "6419"
     :association-rule/country "AUS"
     :association-rule/kind :policy-position
     :association-rule/url "https://www.ausbanking.org.au/about-us/the-aba/"
     :association-rule/url-provenance :official-ausbanking-org-au
     :association-rule/established-date "1997"
     :association-rule/retrieved-at "2026-07-16"
     :association-rule/topic #{:governance}}]})

(defn spec-basis [assoc-slug] (get catalog assoc-slug))

(defn coverage
  ([] (coverage (keys catalog)))
  ([slugs]
   (let [have (filter catalog slugs)
         missing (remove catalog slugs)]
     {:requested (count slugs)
      :covered (count have)
      :covered-associations (vec (sort have))
      :missing-associations (vec (sort missing))
      :note (str "cloud-itonami-assoc-6419-aus-aba Wave 0 (ADR-2607141700): "
                 (count (get catalog "aba")) " aba entries seeded with an "
                 "official ausbanking.org.au citation. Extend "
                 "`association.facts/catalog`, never fabricate a rule id/url.")})))

(defn by-topic [assoc-slug topic]
  (filterv #(contains? (:association-rule/topic %) topic) (spec-basis assoc-slug)))
