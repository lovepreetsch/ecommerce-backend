package com.ecommerce.product.service;

import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.DuplicateResourceException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.*;
import com.ecommerce.product.entity.*;
import com.ecommerce.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Cacheable(value = "products_v2", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public PagedResponse<ProductDTO> getAllProducts(Pageable pageable) {
        Page<ProductDTO> page = productRepository.findAll(pageable).map(this::mapToDTO);
        return toPagedResponse(page);
    }

    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToDTO(product);
    }

    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return mapToDTO(product);
    }

    @Transactional
    @CacheEvict(value = {"products_v2", "product"}, allEntries = true)
    public ProductDTO createProduct(Long vendorId, CreateProductRequest request) {
        String slug = toSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Product product = Product.builder()
                .vendorId(vendorId)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .sku(request.getSku())
                .category(category)
                .status(request.getStatus() != null ?
                        Product.ProductStatus.valueOf(request.getStatus()) : Product.ProductStatus.ACTIVE)
                .build();

        // Add images
        if (request.getImages() != null) {
            request.getImages().forEach(imgDto -> {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(imgDto.getUrl())
                        .altText(imgDto.getAltText())
                        .displayOrder(imgDto.getDisplayOrder())
                        .build();
                product.getImages().add(image);
            });
        }

        // Add attributes
        if (request.getAttributes() != null) {
            request.getAttributes().forEach(attrDto -> {
                ProductAttribute attr = ProductAttribute.builder()
                        .product(product)
                        .name(attrDto.getName())
                        .value(attrDto.getValue())
                        .build();
                product.getAttributes().add(attr);
            });
        }

        Product saved = productRepository.save(product);
        log.info("Product created: {} by vendor {}", saved.getName(), vendorId);
        return mapToDTO(saved);
    }

    @Transactional
    @CacheEvict(value = {"products_v2", "product"}, allEntries = true)
    public ProductDTO updateProduct(Long vendorId, Long productId, CreateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!product.getVendorId().equals(vendorId)) {
            throw new BadRequestException("You can only update your own products");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setSku(request.getSku());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getStatus() != null) {
            product.setStatus(Product.ProductStatus.valueOf(request.getStatus()));
        }

        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Transactional
    @CacheEvict(value = {"products_v2", "product"}, allEntries = true)
    public void deleteProduct(Long vendorId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        if (!product.getVendorId().equals(vendorId)) {
            throw new BadRequestException("You can only delete your own products");
        }
        productRepository.delete(product);
    }

    public PagedResponse<ProductDTO> getProductsByVendor(Long vendorId, Pageable pageable) {
        Page<ProductDTO> page = productRepository.findByVendorId(vendorId, pageable).map(this::mapToDTO);
        return toPagedResponse(page);
    }

    public PagedResponse<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<ProductDTO> page = productRepository.findByCategoryIdAndStatus(categoryId, Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToDTO);
        return toPagedResponse(page);
    }

    public PagedResponse<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        Page<ProductDTO> page = productRepository.searchByKeyword(keyword, pageable).map(this::mapToDTO);
        return toPagedResponse(page);
    }

    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public ReviewDTO addReview(Long productId, Long userId, ReviewDTO reviewDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new DuplicateResourceException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .product(product)
                .userId(userId)
                .rating(reviewDTO.getRating())
                .comment(reviewDTO.getComment())
                .build();
        review = reviewRepository.save(review);

        // Update product rating
        Double avg = reviewRepository.getAverageRating(productId);
        int count = reviewRepository.countByProductId(productId);
        product.setAverageRating(avg != null ? avg : 0.0);
        product.setReviewCount(count);
        productRepository.save(product);

        return mapReviewToDTO(review);
    }

    // ===== Categories =====

    @Cacheable(value = "categories")
    public List<CategoryDTO> getCategoryTree() {
        return categoryRepository.findByParentIsNull().stream()
                .map(this::mapCategoryToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO createCategory(CategoryDTO dto) {
        if (categoryRepository.existsBySlug(dto.getSlug())) {
            throw new DuplicateResourceException("Category", "slug", dto.getSlug());
        }

        Category parent = null;
        if (dto.getParentId() != null) {
            parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", dto.getParentId()));
        }

        Category category = Category.builder()
                .name(dto.getName())
                .slug(dto.getSlug() != null ? dto.getSlug() : toSlug(dto.getName()))
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .parent(parent)
                .build();

        category = categoryRepository.save(category);
        return mapCategoryToDTO(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setName(dto.getName());
        category.setSlug(dto.getSlug() != null ? dto.getSlug() : toSlug(dto.getName()));
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", dto.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        return mapCategoryToDTO(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        categoryRepository.delete(category);
    }

    // ===== Reviews Admin =====

    public PagedResponse<ReviewDTO> getAllReviews(Pageable pageable) {
        Page<ReviewDTO> page = reviewRepository.findAll(pageable).map(this::mapReviewToDTO);
        return toPagedResponse(page);
    }

    @Transactional
    public ReviewDTO updateReviewStatus(Long id, String status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        review.setStatus(Review.ReviewStatus.valueOf(status.toUpperCase()));
        review = reviewRepository.save(review);
        return mapReviewToDTO(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        reviewRepository.delete(review);
    }

    // ===== Mappers =====

    private ProductDTO mapToDTO(Product p) {
        return ProductDTO.builder()
                .id(p.getId())
                .vendorId(p.getVendorId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .price(p.getPrice())
                .compareAtPrice(p.getCompareAtPrice())
                .sku(p.getSku())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .status(p.getStatus().name())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .images(p.getImages().stream().map(img ->
                        ProductDTO.ImageDTO.builder()
                                .id(img.getId()).url(img.getUrl())
                                .altText(img.getAltText()).displayOrder(img.getDisplayOrder())
                                .build()).collect(Collectors.toList()))
                .attributes(p.getAttributes().stream().map(attr ->
                        ProductDTO.AttributeDTO.builder()
                                .id(attr.getId()).name(attr.getName()).value(attr.getValue())
                                .build()).collect(Collectors.toList()))
                .build();
    }

    private CategoryDTO mapCategoryToDTO(Category c) {
        return CategoryDTO.builder()
                .id(c.getId()).name(c.getName()).slug(c.getSlug())
                .description(c.getDescription()).imageUrl(c.getImageUrl())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .children(c.getChildren().stream().map(this::mapCategoryToDTO).collect(Collectors.toList()))
                .build();
    }

    private ReviewDTO mapReviewToDTO(Review r) {
        return ReviewDTO.builder()
                .id(r.getId()).userId(r.getUserId()).rating(r.getRating())
                .comment(r.getComment())
                .status(r.getStatus().name())
                .productName(r.getProduct() != null ? r.getProduct().getName() : null)
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.of(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private String toSlug(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
