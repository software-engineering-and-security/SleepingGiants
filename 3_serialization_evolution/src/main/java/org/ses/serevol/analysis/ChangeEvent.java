package org.ses.serevol.analysis;

import sootup.core.types.ClassType;

public class ChangeEvent {
        public ChangeType changeType;
        public ClassType serializableProvider;

        public enum ChangeType {
            ADD_CLASS, REMOVE_CLASS, ADD_SERIALIZABLE, REMOVE_SERIALIZABLE,
            ADD_SERIALIZABLE_INDIRECT, REMOVE_SERIALIZABLE_INDIRECT
        }

        public ChangeEvent(ChangeType changeType) {
            this.changeType = changeType;
        }

    @Override
    public String toString() {

        String serializableProviderName = serializableProvider == null ? "null" : serializableProvider.getFullyQualifiedName();

        return "\"changeType\":\"" +
                changeType.toString() +  "\",\"serializableProvider\":\"" +
                serializableProviderName + "\"}";
    }


}
