    package com.example.japanesedictionary.ui.components

    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.Surface
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.input.TextFieldValue
    import androidx.compose.ui.unit.dp
    import com.example.japanesedictionary.R
    import com.example.japanesedictionary.viewmodel.DictionaryViewModel

    @Composable
    fun DictionaryHeader(
        query: TextFieldValue,
        onQueryChange: (TextFieldValue) -> Unit,
        onSearch: () -> Unit,
        onSaveIconClick: () -> Unit,
        viewModel: DictionaryViewModel,
        onFocusChanged: (Boolean) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF5F5F5),
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                // Sử dụng component SearchBar riêng biệt
                CustomSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onFocusChanged = onFocusChanged,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Switch chọn chế độ tìm kiếm với kích thước nhỏ gọn hơn
                SearchModeSwitch(viewModel = viewModel)

                Spacer(modifier = Modifier.width(8.dp))

                // Icon lưu nhóm
                IconButton(onClick = onSaveIconClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_bookmark_24),
                        contentDescription = "Save Groups"
                    )
                }
            }
        }
    }