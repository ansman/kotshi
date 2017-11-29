package se.ansman.kotshi;

// Until https://youtrack.jetbrains.com/issue/KT-21433 is resolved this enum has to be written in Java
public enum SomeEnum {
    VALUE1,
    VALUE2,
    @JsonDefaultValue
    VALUE3,
    VALUE4,
    VALUE5
}
