@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi.kapt

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.asTypeVariableName
import com.squareup.kotlinpoet.metadata.*
import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.*
import se.ansman.kotshi.Errors.abstractFactoriesAreDeprecated
import se.ansman.kotshi.model.*
import se.ansman.kotshi.model.JsonAdapterFactory.Companion.getManualAdapter
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass

class FactoryProcessingStep(
    override val processor: KotshiProcessor,
    private val messager: Messager,
    override val filer: Filer,
    private val types: Types,
    private val elements: Elements,
    private val generatedAnnotation: GeneratedAnnotation?,
    private val generatedAdapters: List<GeneratedAdapter>,
    private val metadataAccessor: MetadataAccessor,
    private val createAnnotationsUsingConstructor: Boolean?,
) : KotshiProcessor.GeneratingProcessingStep() {

    @OptIn(ExperimentalKotshiApi::class)
    override val annotations: Set<Class<out Annotation>> = setOf(
        KotshiJsonAdapterFactory::class.java,
        RegisterJsonAdapter::class.java,
    )

    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>,
        roundEnv: RoundEnvironment
    ) {
        val elements = elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
            .map(MoreElements::asType)
        val manuallyRegisteredAdapters = elementsByAnnotation.getManualAdapters(elements.isNotEmpty()).toList()
        if (elements.size > 1) {
            messager.logKotshiError(Errors.multipleFactories(elements.map { it.qualifiedName.toString() }), elements.first())
        } else for (element in elements) {
            try {
                generateFactory(element, manuallyRegisteredAdapters)
            } catch (e: KaptProcessingError) {
                messager.logKotshiError(e)
            }
        }
    }

    private fun generateFactory(element: TypeElement, manuallyRegisteredAdapters: List<RegisteredAdapter>) {
        val elementClassName = createClassName(metadataAccessor.getKmClass(element).name)
        val factory = JsonAdapterFactory(
            targetType = elementClassName,
            usageType = if (
                element.asType().implements(JsonAdapter.Factory::class) &&
                Modifier.ABSTRACT in element.modifiers
            ) {
                messager.logKotshiWarning(abstractFactoriesAreDeprecated, element)
                JsonAdapterFactory.UsageType.Subclass(elementClassName, parentIsInterface = element.kind == ElementKind.INTERFACE)
            } else {
                JsonAdapterFactory.UsageType.Standalone
            },
            generatedAdapters = generatedAdapters,
            manuallyRegisteredAdapters = manuallyRegisteredAdapters,
        )

        val createAnnotationsUsingConstructor =
            createAnnotationsUsingConstructor ?: metadataAccessor.getMetadata(element).supportsCreatingAnnotationsWithConstructor

        JsonAdapterFactoryRenderer(factory, createAnnotationsUsingConstructor)
            .render(generatedAnnotation) {
                addOriginatingElement(element)
            }
            .writeTo(filer)
    }

    @OptIn(DelicateKotlinPoetApi::class, ExperimentalKotshiApi::class)
    private fun SetMultimap<Class<out Annotation>, Element>.getManualAdapters(hasFactory: Boolean): Sequence<RegisteredAdapter> =
        this[RegisterJsonAdapter::class.java]
            .asSequence()
            .map { MoreElements.asType(it) }
            .mapNotNull { element ->
                val kmClass = metadataAccessor.getKmClassOrNull(element) ?: run {
                    messager.logKotshiError(Errors.javaClassNotSupported, element)
                    return@mapNotNull null
                }
                if (!kmClass.isObject && (!kmClass.isClass ||
                        kmClass.flags.isAbstract ||
                        kmClass.flags.isLocal ||
                        kmClass.flags.isInnerClass ||
                        kmClass.flags.isSealed ||
                        kmClass.flags.isEnumClass
                        )
                ) {
                    messager.logKotshiError(Errors.invalidRegisterAdapterType, element)
                    return@mapNotNull null
                }
                if (!kmClass.flags.isPublic && !kmClass.flags.isInternal) {
                    messager.logKotshiError(Errors.invalidRegisterAdapterVisibility, element)
                    return@mapNotNull null
                }

                if (!hasFactory) {
                    messager.logKotshiError(Errors.registeredAdapterWithoutFactory, element)
                }

                val annotation = element.getAnnotation(RegisterJsonAdapter::class.java)!!
                element.getManualAdapter(
                    logError = messager::logKotshiError,
                    getSuperClass = {
                        superclass.takeUnless { it.kind == TypeKind.NONE }?.let(MoreTypes::asTypeElement)
                    },
                    getSuperTypeName = { metadataAccessor.getTypeSpecOrNull(this)?.superclass ?: superclass.asTypeName() },
                    adapterClassName = createClassName(kmClass.name),
                    typeVariables = { metadataAccessor.getTypeSpecOrNull(this)?.typeVariables ?: typeParameters.map { it.asTypeVariableName() } },
                    isObject = kmClass.flags.isObjectClass || kmClass.flags.isCompanionObjectClass,
                    isAbstract = kmClass.flags.isInterface || kmClass.flags.isAbstract || kmClass.flags.isSealed,
                    priority = annotation.priority,
                    getKotshiConstructor = {
                        metadataAccessor.getTypeSpec(this).constructors().findKotshiConstructor(
                            parameters = { parameters },
                            type = { type },
                            hasDefaultValue = { defaultValue != null }
                        ) { name }
                    },
                    getJsonQualifiers = {
                        metadataAccessor.getTypeSpec(this).annotationSpecs.qualifiers(metadataAccessor)
                    }
                )
            }

    private fun TypeMirror.implements(someType: KClass<*>): Boolean =
        types.isSubtype(this, elements.getTypeElement(someType.java.canonicalName).asType())
}