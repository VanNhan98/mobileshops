package vn.smartapple.appleshop.controller.client;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.smartapple.appleshop.domain.Cart;
import vn.smartapple.appleshop.domain.CartDetail;
import vn.smartapple.appleshop.domain.Product;
import vn.smartapple.appleshop.domain.Product_;
import vn.smartapple.appleshop.domain.User;
import vn.smartapple.appleshop.domain.dto.ProductCriteriaDTO;
import vn.smartapple.appleshop.service.ProductService;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ItemController {
    @Autowired
    private ProductService productService;

    @GetMapping("/product/{id}")
    public String getProductDetailPage(Model model, @PathVariable long id) {
        Product product = this.productService.getProductById(id).get();
        model.addAttribute("product", product);
        model.addAttribute("id", id);

        return "client/product/detail";
    }

    @GetMapping("/products")
    public String getProductPage(Model model, ProductCriteriaDTO productCriteriaDTO, HttpServletRequest request) {
        int page = 1;
        try {
            if (productCriteriaDTO.getPage().isPresent()) {
                page = Integer.parseInt(productCriteriaDTO.getPage().get());
            }
        } catch (Exception e) {

        }

        // sort
        Pageable pageable = PageRequest.of(page - 1, 3);
        if (productCriteriaDTO.getSort() != null && productCriteriaDTO.getSort().isPresent()) {
            String sort = productCriteriaDTO.getSort().get();
            if (sort.equals("gia-tang-dan")) {
                pageable = PageRequest.of(page - 1, 3, Sort.by(Product_.PRICE).ascending());
            } else if (sort.equals("gia-giam-dan")) {
                pageable = PageRequest.of(page - 1, 3, Sort.by(Product_.PRICE).descending());
            } else {
                pageable = PageRequest.of(page - 1, 3);
            }
        }

        Page<Product> prs = this.productService.fetchProductWithSpec(pageable, productCriteriaDTO);

        List<Product> listProducts = prs.getContent();

        String qs = request.getQueryString();
        if (qs != null) {
            qs = qs.replace("page=" + page, "");

        }

        model.addAttribute("products", listProducts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", prs.getTotalPages());
        model.addAttribute("queryString", qs);
        return "client/product/show";
    }

    @PostMapping("/add-product-to-cart/{id}")
    public String addProductToCart(Model model, @PathVariable long id, HttpServletRequest request) {
        long productId = id;

        HttpSession session = request.getSession(false);
        String email = (String) session.getAttribute("email");
        this.productService.handleAddProductToCart(email, productId, session, 1);

        return "redirect:/";
    }

    @PostMapping("/add-product-to-cart-from-products/{id}")
    public String addProductToCartFromProducts(Model model, @PathVariable long id, HttpServletRequest request) {
        long productId = id;

        HttpSession session = request.getSession(false);
        String email = (String) session.getAttribute("email");
        this.productService.handleAddProductToCart(email, productId, session, 1);

        return "redirect:/products";
    }

    @PostMapping("/add-product-from-view-detail")
    public String handleAddProductFormViewDetail(
            @RequestParam("id") long id,
            @RequestParam("quantity") long quantity,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String email = (String) session.getAttribute("email");
        this.productService.handleAddProductToCart(email, id, session, quantity);
        return "redirect:/product/" + id;

    }

    @GetMapping("/cart")
    public String getCartPage(Model model, HttpServletRequest request) {
        User currentUser = new User();
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");

        currentUser.setId(id);
        Cart cart = this.productService.findCartByUser(currentUser);
        List<CartDetail> cartDetails = cart == null ? new ArrayList<>() : cart.getCartDetails();
        double totalPrice = 0;
        for (CartDetail cd : cartDetails) {
            totalPrice += cd.getPrice() * cd.getQuantity();
        }

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cart", cart);

        return "client/cart/show";
    }

    @PostMapping("/delete-cart-product/{id}")
    public String deleteCartDetail(@PathVariable long id, HttpServletRequest request) {
        long cartDetailId = id;
        HttpSession session = request.getSession(false);
        session.getAttribute("id");
        this.productService.handleRemoveCartDetail(cartDetailId, session);

        return "redirect:/cart";
    }

    @PostMapping("/confirm-checkout")
    public String getCheckOutPage(Model model, @ModelAttribute("cart") Cart cart) {
        List<CartDetail> cartDetails = cart == null ? new ArrayList<>() : cart.getCartDetails();
        this.productService.handleUpdateCartBeforeCheckout(cartDetails);

        return "redirect:/checkout";
    }

    @GetMapping("/checkout")
    public String getCheckOutPage(Model model, HttpServletRequest request) {
        User currentUser = new User();
        HttpSession session = request.getSession(false);
        long id = (long) session.getAttribute("id");
        currentUser.setId(id);

        Cart cart = this.productService.findCartByUser(currentUser);
        List<CartDetail> cartDetail = cart == null ? new ArrayList<>() : cart.getCartDetails();
        double totalPrice = 0;
        for (CartDetail cd : cartDetail) {
            totalPrice += cd.getPrice() * cd.getQuantity();
        }
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cartDetails", cartDetail);

        return "client/cart/checkout";
    }

    @PostMapping("/place-order")
    public String postMethodName(HttpServletRequest request, @RequestParam("receiverName") String receiverName,
            @RequestParam("receiverAddress") String receiverAddress,
            @RequestParam("receiverPhone") String receiverPhone) {
        HttpSession session = request.getSession(false);
        User user = new User();
        long id = (long) session.getAttribute("id");
        user.setId(id);
        this.productService.handleCheckOutCart(user, session, receiverName, receiverAddress, receiverPhone);

        return "redirect:/thanks";
    }

    @GetMapping("/thanks")
    public String getThanksPage(Model model) {
        return "client/cart/thanks";
    }

}