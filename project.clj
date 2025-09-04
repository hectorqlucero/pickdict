(defproject hectorqlucero/pickdict "0.1.0"
  :description "Pick/D3-style multivalue database layer for SQL databases"
  :url "https://github.com/hectorqlucero/pickdict"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.50.3.0"]
                 [cheshire "6.1.0"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases false}]]
  :scm {:name "git"
        :url "https://github.com/hectorqlucero/pickdict"}
  :pom-addition [:developers [:developer
                              [:name "Hector"]
                              [:email "hector@example.com"]]])
