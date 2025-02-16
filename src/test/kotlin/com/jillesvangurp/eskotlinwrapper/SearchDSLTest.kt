package com.jillesvangurp.eskotlinwrapper

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.jillesvangurp.eskotlinwrapper.documentation.Thing
import com.jillesvangurp.eskotlinwrapper.dsl.ExistsQuery
import com.jillesvangurp.eskotlinwrapper.dsl.FuzzyQuery
import com.jillesvangurp.eskotlinwrapper.dsl.IdsQuery
import com.jillesvangurp.eskotlinwrapper.dsl.MatchBoolPrefixQuery
import com.jillesvangurp.eskotlinwrapper.dsl.MatchOperator
import com.jillesvangurp.eskotlinwrapper.dsl.MatchPhrasePrefixQuery
import com.jillesvangurp.eskotlinwrapper.dsl.MatchQuery
import com.jillesvangurp.eskotlinwrapper.dsl.MultiMatchQuery
import com.jillesvangurp.eskotlinwrapper.dsl.MultiMatchType
import com.jillesvangurp.eskotlinwrapper.dsl.QueryStringQuery
import com.jillesvangurp.eskotlinwrapper.dsl.RangeQuery
import com.jillesvangurp.eskotlinwrapper.dsl.RegExpQuery
import com.jillesvangurp.eskotlinwrapper.dsl.SearchDSL
import com.jillesvangurp.eskotlinwrapper.dsl.SimpleQueryStringQuery
import com.jillesvangurp.eskotlinwrapper.dsl.SortMode
import com.jillesvangurp.eskotlinwrapper.dsl.SortOrder
import com.jillesvangurp.eskotlinwrapper.dsl.TermQuery
import com.jillesvangurp.eskotlinwrapper.dsl.TermsQuery
import com.jillesvangurp.eskotlinwrapper.dsl.ZeroTermsQuery
import com.jillesvangurp.eskotlinwrapper.dsl.bool
import com.jillesvangurp.eskotlinwrapper.dsl.boosting
import com.jillesvangurp.eskotlinwrapper.dsl.matchAll
import com.jillesvangurp.eskotlinwrapper.dsl.sort
import org.elasticsearch.action.search.configure
import org.elasticsearch.action.search.dsl
import org.junit.jupiter.api.Test

class SearchDSLTest : AbstractElasticSearchTest(indexPrefix = "search", createIndex = true) {
    val objectMapper = ObjectMapper()

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `should construct matchAll query`() {
        val s = SearchDSL()
        s.apply {
            resultSize = 10
            from = 0
            query = matchAll()
        }
        assertThat(s["from"]).isEqualTo(0)
        assertThat(s["query"] as Map<String, Any>).hasSize(1)
        assertThat((s["query"] as Map<String, Any>).keys).contains("match_all")
    }

    @Test
    fun `should construct bool`() {
        testQuery {
            query = bool {
                should(
                    MatchQuery("title", "foo"),
                    MatchQuery("title", "quick brown fox") {
                        // ESQuery is a MutableMap that modifies the underlying queryDetails
                        this["boost"] = 0.6
                    }
                )
            }
        }
    }

    @Test
    fun `boosting query`() {
        testQuery {
            query = boosting {
                positive = matchAll()
                negative = MatchQuery("title", "nooo")
                negativeBoost = 0.1
            }
        }
    }

    @Test
    fun `match query`() {
        testQuery {
            query = MatchQuery("title", "foo bar") {
                operator = MatchOperator.AND
                zeroTermsQuery = ZeroTermsQuery.none
            }
        }
    }

    @Test
    fun `match bool prefix query`() {
        testQuery {
            query = MatchBoolPrefixQuery("title", "foo bar") {
                operator = MatchOperator.OR
            }
        }
    }

    @Test
    fun `match phrase prefix query`() {
        testQuery {
            query = MatchPhrasePrefixQuery("title", "foo ba") {
                slop = 3
            }
        }
    }

    @Test
    fun `multi match query`() {
        testQuery {
            query = MultiMatchQuery("foo bar", "title", "description") {
                type = MultiMatchType.best_fields
                fuzziness = "AUTO"
            }
        }
    }

    @Test
    fun `query string query`() {
        testQuery {
            query = QueryStringQuery("foo bar") {
                defaultField = "title"
                fuzziness = "AUTO"
            }
        }
    }

    @Test
    fun `simple query string query`() {
        testQuery {
            query = SimpleQueryStringQuery("foo AND bra", "title", "description") {
                fuzzyTranspositions = true
            }
        }
    }

    @Test
    fun `exists query`() {
        testQuery {
            query = ExistsQuery("title")
        }
    }

    @Test
    fun `fuzzy query`() {
        testQuery {
            query = FuzzyQuery("title", "ofo bra") {
                fuzziness = "AUTO"
            }
        }
    }

    @Test
    fun `ids query`() {
        testQuery {
            query = IdsQuery("1", "2")
        }
    }

    @Test
    fun `range query`() {
        testQuery {
            query = RangeQuery("Title") {
                gt = 42
            }
        }
    }

    @Test
    fun `regexp query`() {
        testQuery {
            query = RegExpQuery("title", "f.*")
        }
    }

    @Test
    fun `term query`() {
        testQuery {
            query = TermQuery("_id", "42")
        }
    }

    @Test
    fun `terms query`() {
        testQuery {
            query = TermsQuery("_id", "42", "43")
        }
    }

    @Test
    fun `add sort fields`() {
        testQuery {
            sort {
                +"tag"
                +TestModel::tag
                add(TestModel::number, order = SortOrder.DESC, mode = SortMode.MAX)
            }
        }
    }

    private fun testQuery(block: SearchDSL.() -> Unit): SearchResults<TestModel> {
        // we test here that ES does not throw some kind of error and accepts the query without validation problems
        // we don't care about the results in this case
        // note we also don't test all parameters
        return repository.search {
            configure(true, block)
        }
    }
}
