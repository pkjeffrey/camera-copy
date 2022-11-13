(defproject camera-copy "0.1.0-SNAPSHOT"
  :description "Copy camera files from SD Card to Photos/Raw"
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :main ^:skip-aot camera-copy.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :uberjar-name "camera-copy.jar"
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
