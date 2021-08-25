package se.ansman.kotshi.kapt

import javax.lang.model.element.Element

class KaptProcessingError(message: String, val element: Element) : Exception(message)