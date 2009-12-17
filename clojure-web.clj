(ns clojure-web 
  (:import (java.io PushbackReader StringReader
                    StringWriter PrintWriter))
  (:use compojure compojure.encodings
        [clojure.contrib.repl-utils :only (get-source)])
)

; records a string (:expr) along with the result of its evaluation by
; repl, and anything written to *out* or *err* 
(defstruct history-item :expr :result :out :err)

; a list of history-items
(def history-items (atom ()))

(defn html-history-item [{:keys [expr result out err]}]
  (html [:tr [:td {:rowspan "2" :valign "top"} expr ]
             [:td {:rowspan "2" :valign "top"} (escape-html result)]
             [:td [:pre (escape-html out)]]]
        [:tr [:td [:pre (escape-html err)]]]
  )
)

(defn html-repl [history]
  (html [:form {:action "/repl", :method "post"}
          [:input {:type "text", :name "expr"}]
          [:input {:type "submit", :value "Submit"}]
        ]
        [:table {:cellpadding "10"}
          (map html-history-item history)
        ]
  )
)

(defn repl-get [request]
  "Handles a GET to /repl, returns a map with the :body key set to 
  a blank html form"
  {:body (html-repl @history-items)}
)

(defn read-eval [expr-str]
"Returns a triple of strings giving the result, and any output to
 stdout or stderr when expr-str is read and evaluated"
  (let [out (StringWriter.)
        err-writer (StringWriter.)
        err (PrintWriter. err-writer)
        result (binding [*out* out, *err* err]
                        (try (eval (read-string expr-str))
                             (catch Exception e (.println err e))
                        )
               )
       ]
       ; note use of pr-str, this prints a clojure object in a form
       ; which is readable by the read function 
       [(pr-str result) (str out) (str err-writer)]
  )
)

(defn repl-post [request]
"Handles a post to /repl, simply doing a read and then eval of the
 relevant string passed in the request parameter. Doesn't handle the
 exceptions potentially thrown by read or eval"
  (let [expr (:expr (:form-params request))
        [result out error] (read-eval expr)
	new-item (struct history-item expr result out error)
	history (swap! history-items conj new-item)
       ]
    {:body (html-repl history)}
  )
)

(defn html-ns [base-uri, ns]
  (let [name (str (ns-name ns))]
    (html [:li [:a {:href (str base-uri "/" name)} name]])
  )
)

(defn all-ns-get [request]
"Returns a list of all namespaces currently seen by this process"
  (let [uri (:uri request)]
    (html [:ol (map (partial html-ns uri)
                    (sort-by #(ns-name %) (all-ns)))])
  )
)

(defn ns-get [request]
  (let [ns (find-ns (symbol (:* (:route-params request))))
        interns (ns-interns ns)
        uri (:uri request)]
    (html [:h1 (ns-name ns)]
          [:ol (for [sym-str (map str (keys interns))]
                    [:li [:a {:href (str uri "/" (urlencode sym-str))                                        } sym-str]])
          ])
  )
)

(defn ns-uri [ns]
"Given a namespace's name the function returns the uri for the
 namespace" 
  (str "/ns/" (ns-name ns))
)

(defn format-meta-map [m]
  (let [m-esc (zipmap (keys m) (map #(escape-html (str %)) (vals m)))
        all-formatters
          {:ns #(html [:a {:href (ns-uri %)} (ns-name %)])
           :doc #(html [:pre %])}
        some-formatters (select-keys all-formatters (keys m))
        apply-format #((some-formatters %) (m %))
        formatted-values (map apply-format (keys some-formatters))] 
    (merge m-esc (zipmap (keys some-formatters) formatted-values))
  )
)

(defn html-map [m]
"Converts a map to a html definition list"
  (let [dts (map #(vector :dt (str %)) (keys m))
        dds (map #(vector :dd (str %)) (vals m))]
    (html [:dl (interleave dts dds)])
  )
)

(defn html-var [var]
  (let [name (:name ^var)
        full-name (symbol (str (ns-name (:ns ^var)) "/" name))
        metadata (html-map (format-meta-map ^var)) ]
    (html [:body [:h1 name] [:pre (get-source full-name)]
                 (html-map {(html [:h3 "Metadata"]) metadata})
          ]) 
  )
)

(defn symbol-get [request]
  (let [[ns-str, sym-str] (:* (:route-params request))
         sym (symbol sym-str)
         ns (find-ns (symbol ns-str))]
    (html-var (ns-resolve ns sym)) 
  )
)

(defroutes clojure-web
  (GET "/" (html [:h1 "Clojure " [:a {:href"/repl"} "REPL"]]))
  (GET "/repl" repl-get)
  (POST "/repl" repl-post)
  (GET "/ns" all-ns-get)
  (GET "/ns/*/*" symbol-get)
  (GET "/ns/*" ns-get)
)


(defn run []
  (run-server {:port 8080}
    "/*" (servlet clojure-web))
)

(run)
