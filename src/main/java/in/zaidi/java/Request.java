package com.sapient.jackbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {
	private String name, entityId, spaceId, environment, cotentType, oldAssetId, copyText;
}
