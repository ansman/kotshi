package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.ObjectJsonAdapter

class ObjectAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, element, globalConfig) {
    init {
        require(element.classKind == ClassKind.OBJECT)
    }

    override fun getGenerableAdapter(): GeneratableJsonAdapter =
        ObjectJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            polymorphicLabels = polymorphicLabels,
        )
}