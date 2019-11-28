package com.example.demoorderservice;

import java.util.UUID;

import javax.inject.Inject;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;

import com.example.democore.CreateInitiateShippingCommand;
import com.example.democore.CreateInvoiceCommand;
import com.example.democore.CreateShippingCommand;
import com.example.democore.InitiateOrderShippedEvent;
import com.example.democore.InvoiceCreatedEvent;
import com.example.democore.OrderCreatedEvent;
import com.example.democore.OrderShippedEvent;
import com.example.democore.OrderShippedFailedEvent;
import com.example.democore.OrderUpdatedEvent;
import com.example.democore.UpdateOrderStatusCommand;

@Saga
public class OrderManagementSaga {

	@Inject
	private transient CommandGateway commandGateway;

	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderCreatedEvent orderCreatedEvent) {
		String paymentId = UUID.randomUUID().toString();
		System.out.println("Saga invoked");

		// associate Saga
		SagaLifecycle.associateWith("paymentId", paymentId);

		System.out.println("order id" + orderCreatedEvent.orderId);

		// send the commands
		commandGateway
				.send(new CreateInvoiceCommand(paymentId, orderCreatedEvent.orderId, orderCreatedEvent.userToken));
	}

	@SagaEventHandler(associationProperty = "paymentId")
	public void handle(InvoiceCreatedEvent invoiceCreatedEvent) {
		String shippingId = UUID.randomUUID().toString();

		System.out.println("Saga continued");

		// associate Saga with shipping
		SagaLifecycle.associateWith("shipping", shippingId);

		// send the create shipping command
		commandGateway.send(new CreateInitiateShippingCommand(shippingId, invoiceCreatedEvent.orderId,
				invoiceCreatedEvent.paymentId, invoiceCreatedEvent.userToken));
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(InitiateOrderShippedEvent initiateOrderShippedEvent) {
		String shippingId = UUID.randomUUID().toString();

		System.out.println("Saga continued initiateOrderShippedEvent");

		// associate Saga with shipping
		SagaLifecycle.associateWith("shipping", shippingId);

		// send the create shipping command
		commandGateway.send(new CreateShippingCommand(shippingId, initiateOrderShippedEvent.orderId,
				initiateOrderShippedEvent.paymentId, initiateOrderShippedEvent.userToken));
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderShippedEvent orderShippedEvent) {
		commandGateway.send(new UpdateOrderStatusCommand(orderShippedEvent.orderId, String.valueOf(OrderStatus.SHIPPED),
				orderShippedEvent.userToken));
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderShippedFailedEvent orderShippedFailedEvent) {
		commandGateway.send(new UpdateOrderStatusCommand(orderShippedFailedEvent.orderId,
				String.valueOf(OrderStatus.REJECTED), orderShippedFailedEvent.userToken));
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderUpdatedEvent orderUpdatedEvent) {
		SagaLifecycle.end();
	}
}
