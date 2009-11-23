(ns hello-www
  (:import (java.io PushbackReader StringReader
                    StringWriter PrintWriter))
  (:use compojure)
)


;(def in-str "")
;(def in (PushbackReader. (StringReader. in-str)))
;(def out (StringWriter. ))
;(def err-str-writer (StringWriter. ))
;(def err (PrintWriter. err-str-writer))

; records a string (:expr) along with the result of its evaluation by
; repl, and anything written to *out* or *err* 
(defstruct history-item :expr :result :out :err)

; a list of history-items
(def history-items (atom ()))

(defn add-links [expr-str]

)

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

(defn all-ns-get [request]
"Returns a list of all namespaces currently seen by this process"
  (map ns-name (all-ns))
)


(defroutes clojure-web
  (GET "/" (html [:h1 "Clojure " [:a {:href"/repl"} "REPL"]]))
  (GET "/repl" repl-get)
  (POST "/repl" repl-post)
  (GET "/all-ns" all-ns-get)
)


(defn run []
  (run-server {:port 8080}
    "/*" (servlet clojure-web))
)

(run)

