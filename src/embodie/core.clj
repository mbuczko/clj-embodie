(ns embodie.core
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [embodie.oembed :refer [find-oembed-json-link find-oembed-provider compose-oembed-link]]
            [embodie.opengraph :refer [has-meta-og tag->map]]))

(def ^:const default-providers [:oembed :open-graph :html])

(def ^:const default-opts {:max-images 3})

(defprotocol Provider
  (fetch [this resource url]))

(defrecord OEmbedProvider [opts]
  Provider
  (fetch [this resource url]
    (when-let [{loc :url discovery :discovery} (find-oembed-provider url)]
      (when-let [location (if discovery
                            (find-oembed-json-link resource)
                            (compose-oembed-link loc url))]
        (:body
         (http/get location {:accept :json :as :json}))))))

(defrecord OpenGraphProvider [opts]
  Provider
  (fetch [this resource url]
    (->> (html/select resource [:head :meta])
         (filter has-meta-og)
         (reduce tag->map {}))))

(defrecord HtmlProvider [opts]
  Provider
  (fetch [this resource url]
    (let [limit  (or (:max-images opts) 1)
          title  (html/select resource [:head :> :title])
          images (html/select resource [:body :img])]
      {:title  (->> (first title) :content)
       :images (->> (take limit images)
                    (map (comp :src :attrs)))})))

;; memoized builders

(def create-oembed-provider
  (memoize (fn [opts] (OEmbedProvider. opts))))
(def create-open-graph-provider
  (memoize (fn [opts] (OpenGraphProvider. opts))))
(def create-html-provider
  (memoize (fn [opts] (HtmlProvider. opts))))


(defmulti from-provider (fn [[provider _]]
                          (identity provider)))

(defmethod from-provider :oembed [[_ opts]]
  (create-oembed-provider opts))

(defmethod from-provider :open-graph [[_ opts]]
  (create-open-graph-provider opts))

(defmethod from-provider :html [[_ opts]]
  (create-html-provider opts))


(defn fetch-resource [url]
  (when-let [body (:body (http/get url nil))]
    (html/html-resource
     (java.io.StringReader. body))))

(defn fetch
  ([url]
   (fetch url default-providers default-opts))
  ([url providers & opts]
   (when-let [res (fetch-resource url)]
     (reduce (fn [reduced provider]
               (assoc reduced
                      provider
                      (.fetch (from-provider [provider (first opts)]) res url))) {} providers))))
