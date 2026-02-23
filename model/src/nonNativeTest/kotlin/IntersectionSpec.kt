import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import software.consistent.model.Intersection
import software.consistent.model.MString
import software.consistent.model.MType

class IntersectionSpec :
    DescribeSpec({
        describe(Intersection::class.simpleName!!) {
            describe(Intersection::normalize.name) {
                context("applied to any type with itself returns itself") {
                    withData<MType>({ "$it" }, TestTypes.all) { type ->
                        expect(type intersect type).feature(MType::normalize).toEqual(type)
                    }
                }

                // TODO not yet implemented
                xcontext("applied to a type and its supertype returns the type") {
                    withData<TestTypes.WithSupertype>(
                        { "${it.type} & ${it.supertype}" },
                        TestTypes.withSupertypes,
                    ) { (type, supertype) ->
                        expect(type intersect supertype).feature(MType::normalize).toEqual(type)
                    }

                    context("is symmetric") {
                        withData<TestTypes.WithSupertype>(
                            { "${it.supertype} & ${it.type}" },
                            TestTypes.withSupertypes,
                        ) { (type, supertype) ->
                            expect(supertype intersect type).feature(MType::normalize).toEqual(type)
                        }
                    }
                }

                it("flattens itself") {
                    expect((MString("foo") intersect MString("bar")) intersect MString("baz"))
                        .feature(MType::normalize)
                        .toEqual(Intersection(MString("foo"), MString("bar"), MString("baz")))
                }
            }
        }
    })
