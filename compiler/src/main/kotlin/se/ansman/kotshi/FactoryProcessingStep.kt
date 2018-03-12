package se.ansman.kotshi

import com.google.common.collect.SetMultimap
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class FactoryProcessingStep(
    private val messager: Messager,
    private val filer: Filer,
    private val types: Types,
    private val elements: Elements,
    private val sourceVersion: SourceVersion,
    private val adapters: Map<TypeName, GeneratedAdapter>
) : KotshiProcessor.ProcessingStep {

    private fun TypeMirror.implements(someType: KClass<*>): Boolean =
        types.isSubtype(this, elements.getTypeElement(someType.java.canonicalName).asType())

    override val annotations: Set<Class<out Annotation>> = setOf(KotshiJsonAdapterFactory::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment) {
        val elements = elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
        if (elements.size > 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Multiple classes found with annotations KotshiJsonAdapterFactory")
        } else {
            for (element in elements) {
                generateFactory(element)
            }
        }
    }

    private fun generateFactory(element: Element) {
        val factoryType = element as TypeElement
        val generatedName = ClassName.get(factoryType).let {
            ClassName.get(it.packageName(), "Kotshi${it.simpleNames().joinToString("_")}")
        }

        val (genericAdapters, regularAdapters) = adapters.entries.partition { it.value.requiresTypes }

        val typeSpecBuilder = TypeSpec.classBuilder(generatedName.simpleName())
                .maybeAddGeneratedAnnotation(elements, sourceVersion)

        if (element.asType().implements(JsonAdapter.Factory::class)) {
            if (Modifier.ABSTRACT !in element.modifiers) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Must be abstract", element)
            }
            typeSpecBuilder
                .addModifiers(Modifier.FINAL)
                .superclass(TypeName.get(factoryType.asType()))
        } else {
            typeSpecBuilder
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(JsonAdapter.Factory::class.java)
        }

        typeSpecBuilder
            // Package private constructor
            .addMethod(MethodSpec.constructorBuilder()
                .build())
            .addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(ParameterizedTypeName.get(ClassName.get(JsonAdapter::class.java), WildcardTypeName.subtypeOf(TypeName.OBJECT)))
                .addParameter(Type::class.java, "type")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Set::class.java), WildcardTypeName.subtypeOf(Annotation::class.java)), "annotations")
                .addParameter(TypeName.get(Moshi::class.java), "moshi")
                .addStatement("if (!annotations.isEmpty()) return null")
                .addCode("\n")
                .apply {
                    when {
                        genericAdapters.isEmpty() -> addCode(handleRegularAdapters(regularAdapters))
                        regularAdapters.isEmpty() -> addCode(handleGenericAdapters(genericAdapters))
                        else -> {
                            addIf("type instanceof \$T", ParameterizedType::class.java) {
                                addCode(handleGenericAdapters(genericAdapters))
                            }
                            addCode(handleRegularAdapters(regularAdapters))
                        }
                    }
                }
                .addStatement("return null")
                .build())

        JavaFile.builder(generatedName.packageName(), typeSpecBuilder.build()).build().writeTo(filer)
    }

    private fun handleGenericAdapters(adapters: List<Map.Entry<TypeName, GeneratedAdapter>>): CodeBlock = CodeBlock.builder()
        .addStatement("\$1T parameterized = (\$1T) type", ParameterizedType::class.java)
        .addStatement("\$T rawType = parameterized.getRawType()", Type::class.java)
        .apply {
            for ((type, adapter) in adapters) {
                addIf("rawType.equals(\$T.class)", (type as ParameterizedTypeName).rawType) {
                    addStatement("return new \$T<>(moshi, parameterized.getActualTypeArguments())",
                        adapter.className)
                }
            }
        }
        .build()

    private fun handleRegularAdapters(adapters: List<Map.Entry<TypeName, GeneratedAdapter>>): CodeBlock = CodeBlock.builder()
        .apply {
            for ((type, adapter) in adapters) {
                addIf("type.equals(\$T.class)", type) {
                    if (adapter.requiresMoshi) {
                        addStatement("return new \$T(moshi)", adapter.className)
                    } else {
                        addStatement("return new \$T()", adapter.className)
                    }
                }
            }
        }
        .build()

}