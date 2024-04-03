$(document).ready(function() {
    const searchResultsTableBody = $('.search-results tbody');
    let currentPage = 1;
    let totalPages = 1;
    const searchPath = $('[search-path]').attr('search-path') || '/content/project';
    const searchLinkInput = $('#search-link');
    const searchForm = $('.search-form');
    const validationErrorMessage = $('.validation-error-message');
    const successMessage = $('.success-message');
    const noResultsMessage = $('.no-results');
    const totalLinksElement = $('.total-links');
    const pagination = $('.pagination');
    const prevPageButton = pagination.find('.prev-page');
    const nextPageButton = pagination.find('.next-page');
    const currentPageElement = $('.current-page');
    const totalPagesElement = $('.total-pages');

    searchLinkInput.on('input', handleSearchInputChange);

    searchForm.on('submit', handleSearchFormSubmit);

    pagination.on('click', '.prev-page', handlePrevPageClick);
    pagination.on('click', '.next-page', handleNextPageClick);

    function handleSearchInputChange() {
        const isValid = validateUrl(searchLinkInput.val());
        toggleValidationMessage(!isValid);
        toggleFormValidity(isValid);
    }

    function handleSearchFormSubmit(e) {
        e.preventDefault();
        resetSearchResults();
        currentPage = 1;
        updatePagination();
        searchLinks(searchLinkInput.val(), currentPage);
    }

    function handlePrevPageClick() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
            loadPage(currentPage);
        }
    }

    function handleNextPageClick() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
            loadPage(currentPage);
        }
    }

    function toggleValidationMessage(show) {
        validationErrorMessage.toggle(show);
    }

    function toggleFormValidity(isValid) {
        const formGroup = searchLinkInput.closest('.form-group');
        formGroup.find('label').toggleClass('invalid', !isValid);
        searchLinkInput.toggleClass('is-invalid', !isValid);
        searchForm.find('button[type="submit"]').prop('disabled', !isValid);
    }

    function validateUrl(url) {
        const urlPattern = /^(https?:\/\/)?((([a-z\d]([a-z\d-]*[a-z\d])*)\.)+[a-z]{2,}|((\d{1,3}\.){3}\d{1,3}))(\/[-a-z\d%_.~+]*)*(\?[;&a-z\d%_.~+=-]*)?(#[-a-z\d_]*)?$/i;
        return urlPattern.test(url) && url.startsWith("https://www.project.com");
    }

    function resetSearchResults() {
        searchResultsTableBody.empty();
        successMessage.hide();
        noResultsMessage.hide();
    }

    function loadPage(page) {
        searchLinks(searchLinkInput.val(), page);
    }

    function searchLinks(link, page) {
        resetSearchResults();
        $.ajax({
            url: '/bin/project/searchlinks.json',
            data: {
                path: searchPath,
                page: page,
                link: link
            },
            type: 'GET',
            dataType: 'json',
            success: handleSearchSuccess,
            error: handleSearchError
        });
    }

    function handleSearchSuccess(data, status, xhr) {
        if (data.length > 0) {
            successMessage.show();
            data.forEach(function(link) {
                searchResultsTableBody.append(`<tr><td>${link.url}</td><td>${link.path}</td></tr>`);
            });
        } else {
            noResultsMessage.show();
        }
        totalPages = xhr.getResponseHeader('X-Total-Pages');
        totalPagesElement.text(totalPages);
        totalLinksElement.text(xhr.getResponseHeader('X-Total-Links'));
    }

    function handleSearchError(xhr, status, error) {
        console.error('Error fetching data:', error);
        noResultsMessage.show();
    }

    function updatePagination() {
        currentPageElement.text(currentPage);
        totalPagesElement.text(totalPages);
    }
});