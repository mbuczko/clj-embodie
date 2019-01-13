(set-env!
 :source-paths #{"src" "test"}
 :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                 [metosin/boot-alt-test "0.3.2" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [com.taoensso/nippy "2.14.0"]
                 [enlive "1.1.6"]
                 [cheshire "5.8.1"]
                 [clj-http "3.9.1"]])

;; to check the newest versions:
;; boot -d boot-deps ancient

(def +version+ "1.0.0")

(require '[metosin.boot-alt-test :refer [alt-test]]
         '[adzerk.bootlaces :refer [bootlaces! build-jar push-release]])

(task-options! pom {:project 'defunkt/embodie
                    :version +version+
                    :description "Fetching site-embedded data"
                    :url "https://github.com/mbuczko/embodie"
                    :scm {:url "https://github.com/mbuczko/embodie"}})
