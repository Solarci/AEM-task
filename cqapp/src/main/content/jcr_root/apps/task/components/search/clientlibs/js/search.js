$(document).ready(function() {
    $('.search-results tbody').empty();
    var currentPage = 1;
    var totalPages = 1;

    $('#search-link').on('input', function() {
        var isValid = validateUrl($(this).val());
        $(this).closest('.form-group').find('label').toggleClass('invalid', !isValid);
        $(this).toggleClass('is-invalid', !isValid);
        $(this).closest('form').find('button[type="submit"]').prop('disabled', !isValid);
        if (!isValid) {
            $('.validation-error-message').show();
        } else {
            $('.validation-error-message').hide();
        }
    });

    $('.search-form').on('submit', function(e) {
        $('.search-results tbody').empty();
        e.preventDefault();
        var searchPath = $('#search-link').val();
        currentPage = 1;
        searchLinks(searchPath, currentPage);
    });


    function validateUrl(url) {
        var pattern = new RegExp('^(https?:\\/\\/)?' +
            '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' +
            '((\\d{1,3}\\.){3}\\d{1,3}))' +
            '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' +
            '(\\?[;&a-z\\d%_.~+=-]*)?' +
            '(\\#[-a-z\\d_]*)?$', 'i');
        return pattern.test(url) && url.startsWith("https://www.task.com");
    }


    function loadPage(page) {
        var searchPath = $('#search-link').val();
        searchLinks(searchPath, page);
    }

    function searchLinks(searchPath, page) {
        $('.search-results tbody').empty();
        $.ajax({
            url: '/bin/myproject/searchlinks.json?path=' + searchPath + '&page=' + page,
            type: 'GET',
            dataType: 'json',

            success: function(data, status, xhr) {
                $('.no-results').hide();
                if (data.length > 0) {
                    $('.success-message').show();
                    $('.no-results').hide();
                    data.forEach(function(link) {
                        $('.search-results tbody').append('<tr><td>' + link.url + '</td><td>' + link.path + '</td></tr>');
                    });
                } else {
                    $('.success-message').hide();
                    $('.no-results').show();
                }
                totalPages = xhr.getResponseHeader('X-Total-Pages');
                $('.total-pages').text(totalPages);
            },

            error: function(xhr, status, error) {
                console.error('Error fetching data:', error);
                $('.success-message').hide();
                $('.no-results').show();
            }
        });
    }
    $('.pagination').on('click', '.prev-page', function() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
            loadPage(currentPage);
        }
    });

    $('.pagination').on('click', '.next-page', function() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
            loadPage(currentPage);
        }
    });

    function updatePagination() {
        $('.current-page').text(currentPage);
        $('.total-pages').text(totalPages);
    }
});