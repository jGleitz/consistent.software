import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import software.consistent.model.MString
import software.consistent.model.MType
import software.consistent.model.Union

class UnionSpec :
    DescribeSpec({
        describe(Union::class.simpleName!!) {
            describe(Union::normalize.name) {
                context("applied to any type with itself returns itself") {
                    withData<MType>({ "$it" }, TestTypes.all) { type ->
                        expect(type union type).feature(MType::normalize).toEqual(type)
                    }
                }

                // TODO not implemented yet
                xcontext("applied to a type and its supertype returns the supertype") {
                    withData<TestTypes.WithSupertype>(
                        { "${it.type} | ${it.supertype}" },
                        TestTypes.withSupertypes,
                    ) { (type, supertype) ->
                        expect(type union supertype).feature(MType::normalize).toEqual(supertype)
                    }

                    context("is symmetric") {
                        withData<TestTypes.WithSupertype>(
                            { "${it.supertype} | ${it.type}" },
                            TestTypes.withSupertypes,
                        ) { (type, supertype) ->
                            expect(supertype union type).feature(MType::normalize).toEqual(supertype)
                        }
                    }
                }

                it("flattens itself") {
                    expect((MString("foo") union MString("bar")) union MString("baz"))
                        .feature(MType::normalize)
                        .toEqual(Union(MString("foo"), MString("bar"), MString("baz")))
                }
            }
        }
    })
