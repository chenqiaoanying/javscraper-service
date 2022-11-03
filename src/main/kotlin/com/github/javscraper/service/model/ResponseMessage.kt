package com.github.javscraper.service.model

class ErrorInfo(val code: Int, val message: String)

class ResponseMessage<T> private constructor(
    val data: T?,
    val error: ErrorInfo?
) {
    companion object {
        fun <T> success(data: T): ResponseMessage<T> = ResponseMessage(data, null)
        fun <T> error(throwable: Throwable): ResponseMessage<T> = ResponseMessage(null, ErrorInfo(1000, formatMessage(throwable)))
        fun <T> empty(): ResponseMessage<T> = ResponseMessage(null, null)

        private fun formatMessage(throwable: Throwable): String {
            val stringBuilder = StringBuilder()
            stringBuilder.appendLine(throwable.message)
            var nestedError = throwable.cause
            while (nestedError != null) {
                stringBuilder.appendLine("cause by: ${nestedError.message}")
                nestedError = nestedError.cause
            }
            return stringBuilder.toString()
        }
    }
}