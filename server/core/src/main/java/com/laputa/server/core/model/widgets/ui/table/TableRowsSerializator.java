package com.laputa.server.core.model.widgets.ui.table;

import com.laputa.utils.structure.TableLimitedQueue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * This class is used to avoid ConcurrentModificationException for LinkedList.
 * As row field of table class is updated in worker thread
 * while serialization and iterations happens in another thread.
 *
 * Not the best and optimal way. But it is very easy to implement.
 *
 * Temporary fix.
 *
 * //todo find better solution
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 08.09.16.
 */
public class TableRowsSerializator extends JsonSerializer<TableLimitedQueue<Row>> {

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(TableLimitedQueue<Row> value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        jsonGenerator.writeStartArray();
        if (value != null && value.size() > 0) {
            TableLimitedQueue<Row> clonedRows = (TableLimitedQueue<Row>) value.clone();
            for (Row row : clonedRows) {
                jsonGenerator.writeObject(row);
            }
        }
        jsonGenerator.writeEndArray();
    }

}
