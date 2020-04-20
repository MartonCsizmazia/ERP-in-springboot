package com.codecool.erpspringboot2.service;

import com.codecool.erpspringboot2.model.*;
import com.codecool.erpspringboot2.repository.IncomingDeliveryRepository;
import com.codecool.erpspringboot2.repository.LineitemRepository;
import com.codecool.erpspringboot2.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IncomingDeliveryService {

    @Autowired
    private IncomingDeliveryRepository incomingDeliveryRepository;

    @Autowired
    private LineitemRepository lineitemRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    public List<IncomingDelivery> getAllIncomingDelivery(){
        return incomingDeliveryRepository.findAll();
    }

    public List<IncomingDelivery> getAllUncompletedIncomingDelivery(){
        return incomingDeliveryRepository.findAllByStatusNotLike(Status.COMPLETED);
    }

    public IncomingDelivery getIncomingDeliveryById(Long id){
        return incomingDeliveryRepository.findAllById(id).get(0);
    }

    public void addToInventory(IncomingDelivery incomingDelivery){
        for (Lineitem incomingLineitem : incomingDelivery.getIncomingLineitems()) {
            System.out.println(incomingLineitem.getProduct().getName());
            System.out.println(incomingLineitem.getQuantity());

        }
        System.out.println(incomingDelivery.getIncomingLineitems().size());


        mergeToStockRepository(incomingDelivery);
    }



    public void mergeToStockRepository(IncomingDelivery incomingDelivery) {
        Stock stock = stockService.getStock();

        List<Lineitem> newStockLineitems = new ArrayList<>();

        List<Long> idOfNewStockProducts = new ArrayList<>();

        List<Long> idOfStockProducts = new ArrayList<>();
        for (Lineitem stockLineitem : stock.getStockLineitems()) {
            idOfStockProducts.add(stockLineitem.getProduct().getId());
        }

        for (Lineitem incomingLineitem : incomingDelivery.getIncomingLineitems()) {
            if(idOfNewStockProducts.contains(incomingLineitem.getProduct().getId())){
                for (Lineitem newStockLineitem : newStockLineitems) {
                    if (newStockLineitem.getProduct().getId() ==incomingLineitem.getProduct().getId()){
                        int sum = newStockLineitem.getQuantity();
                        sum += incomingLineitem.getQuantity();
                        incomingLineitem.setMergedToStock(true);
                        newStockLineitem.setQuantity(sum);
                        break;
                    }
                }
            }
            else if(idOfStockProducts.contains(incomingLineitem.getProduct().getId())){
                for (Lineitem stockLineitem : stock.getStockLineitems()) {
                    if (stockLineitem.getProduct().getId() ==incomingLineitem.getProduct().getId()){
                        int sum = stockLineitem.getQuantity();
                        sum += incomingLineitem.getQuantity();
                        incomingLineitem.setMergedToStock(true);
                        stockLineitem.setQuantity(sum);
                        break;
                    }
                }
            } else {
                System.out.println("BUILDING");
                Lineitem lineitem = Lineitem.builder()
                        .product(incomingLineitem.getProduct())
                        .quantity(incomingLineitem.getQuantity())
                        .mergedToStock(true)
                        .build();
                newStockLineitems.add(lineitem);
                idOfNewStockProducts.add(lineitem.getProduct().getId());
            }
        }
        System.out.println("ADDING");
        stock.getStockLineitems().addAll(newStockLineitems);
        stockRepository.save(stock);
    }






    public void incomingCompleted(IncomingDelivery incomingDelivery) throws Exception {
        if(incomingDelivery.getStatus()==Status.COMPLETED){
            throw new Exception("This delivery is already added");
        } else {
            addToInventory(incomingDelivery);
            incomingDelivery.setStatus(Status.COMPLETED);
            incomingDeliveryRepository.save(incomingDelivery);
        }

    }


    public IncomingDelivery addIncomingDelivery(IncomingDelivery paramIncomingDelivery){
        IdCreator.fakeDeliveryNumber += 1;
        List<Lineitem> paramincomingLineitems = paramIncomingDelivery.getIncomingLineitems();
        List<Lineitem> incomingLineitems = new ArrayList<>();
        for (Lineitem incomingLineitem : paramincomingLineitems) {
            Lineitem lineitem = Lineitem.builder()
                    .fakeDeliveryKey(IdCreator.fakeDeliveryNumber)
                    .product(incomingLineitem.getProduct())
                    .quantity(incomingLineitem.getQuantity())
                    .build();
            incomingLineitems.add(lineitem);
            //this.lineitemRepository.save(lineitem);
            //NE SAVELD, KÜLÖNBEN
            //PersistentObjectException: detached entity passed to persist
        }

        IncomingDelivery incomingDelivery = IncomingDelivery.builder()
                .fakePrimaryKey(IdCreator.fakeDeliveryNumber)
                .status(paramIncomingDelivery.getStatus())
                .incomingLineitems(incomingLineitems)
                .build();

        /*NOT WORKING(stack owerlow)
        for (Lineitem incomingLineitem : incomingDelivery.getIncomingLineitems()) {
            incomingLineitem.setIncomingDelivery(incomingDelivery);
        }
         */


        this.incomingDeliveryRepository.save(incomingDelivery);
        System.out.println("THIS IS AND ID "+incomingDelivery.getId());

        for (Lineitem incomingLineitem : lineitemRepository.getAllByFakeDeliveryKey(incomingDelivery.getFakePrimaryKey())) {
            incomingLineitem.setIDofIncomingDelivery(incomingDelivery.getId());
            //incomingLineitem.setIncomingDelivery(incomingDelivery); //NOT WORKING(stack owerlow)
            lineitemRepository.save(incomingLineitem);
        }

        return incomingDelivery;
    }
}
