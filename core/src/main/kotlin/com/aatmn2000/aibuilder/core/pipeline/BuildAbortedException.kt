package com.aatmn2000.aibuilder.core.pipeline

/** Thrown when an agent fails and the build cannot continue. */
class BuildAbortedException(message: String) : RuntimeException(message)
