package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import se.ansman.kotshi.Errors
import se.ansman.kotshi.ksp.logKotshiError
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.ObjectJsonAdapter

class ObjectAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig,
    resolver: Resolver
) : AdapterGenerator(environment, element, globalConfig, resolver) {
    init {
        require(element.classKind == ClassKind.OBJECT)
        if (Modifier.DATA !in element.modifiers && environment.apiVersion >= KotlinVersion(1, 9)) {
            environment.logger.logKotshiError(Errors.nonDataObject, element)
        }
    }

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter =
        ObjectJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            polymorphicLabels = polymorphicLabels,
        )
}